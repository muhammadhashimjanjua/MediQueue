package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import mediqueue.auth.AuthService.Role;
import mediqueue.model.Appointment;
import mediqueue.model.Doctor;
import mediqueue.model.TriageCategory;
import mediqueue.model.TriageEntry;
import mediqueue.service.BookingService;
import mediqueue.service.TriageService;
import mediqueue.service.TriageService.QueuePolicy;
import mediqueue.thread.QueueMonitorThread;
import mediqueue.thread.QueuePolicySimulator;

/**
 * MediQueue Main Dashboard.
 * Enhanced Modern Aesthetic Interface:
 * 1. Slot & Appointment Booking with Double-Booking check (Feature 1)
 * 2. Manchester Triage System (MTS) Check-In & Priority Queue with Anti-Starvation Fairness Rule (Feature 2)
 */
public class DashboardFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color BUTTON_COLOR = new Color(0x0F766E);
    private static final Color BUTTON_HOVER = new Color(0x115E59);
    private static final Color BUTTON_ACCENT = new Color(0xD97706);
    private static final Color BUTTON_ACCENT_HOVER = new Color(0xB45309);
    private static final Color BG_LIGHT = new Color(0xF3F4F6);
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_DARK = new Color(0x1F2937);
    private static final Color TEXT_MUTED = new Color(0x6B7280);

    private final String currentUser;
    private final Role currentRole;

    private final BookingService bookingService = new BookingService();
    private final TriageService triageService = new TriageService();

    // Booking Tab Components
    private final ModernTextField txtPatientName = new ModernTextField("Enter patient full name");
    private final JComboBox<Doctor> cmbDoctors = new JComboBox<Doctor>();
    private final ModernTextField txtDate = new ModernTextField("YYYY-MM-DD");
    private final JComboBox<String> cmbSlots = new JComboBox<String>();
    private final JComboBox<String> cmbApptType = new JComboBox<String>(new String[]{"SCHEDULED", "WALK_IN"});
    private final StyledButton btnBook = new StyledButton("Confirm & Book Slot", BUTTON_COLOR, BUTTON_HOVER, 8);
    private final DefaultTableModel modelAppointments = new DefaultTableModel(
            new String[]{"ID", "Patient", "Doctor", "Date", "Slot", "Type", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblAppointments = new JTable(modelAppointments);

    // Booking KPI Metric Labels
    private final JLabel lblMetricTotalAppts = new JLabel("0");
    private final JLabel lblMetricScheduled = new JLabel("0");
    private final JLabel lblMetricWalkIns = new JLabel("0");

    // Triage Tab Components
    private final JComboBox<String> cmbPendingAppts = new JComboBox<String>();
    private final ModernTextField txtTriagePatient = new ModernTextField("Patient full name");
    private final JComboBox<Doctor> cmbTriageDoctor = new JComboBox<Doctor>();
    private final JComboBox<TriageCategory> cmbTriageCategory = new JComboBox<TriageCategory>(TriageCategory.values());
    private final ModernTextField txtComplaint = new ModernTextField("e.g. Chest pain, high fever, dizziness");
    private final JPanel pnlTriagePreview = new JPanel(new BorderLayout(8, 0));
    private final JLabel lblTriagePreviewText = new JLabel();

    private final StyledButton btnCheckIn = new StyledButton("Complete Triage Check-In", BUTTON_COLOR, BUTTON_HOVER, 8);
    private final StyledButton btnCallNext = new StyledButton("Call Next Patient / Mark Served", BUTTON_ACCENT, BUTTON_ACCENT_HOVER, 8);
    private final JLabel lblQueueStats = new JLabel("Queue Status: Loading...");

    // Anti-Starvation & Queue Policy Components
    private QueuePolicy currentPolicy = QueuePolicy.PRIORITY_WITH_AGING;
    private int simulatedAgingMinutes = 0;
    private final JComboBox<QueuePolicy> cmbPolicy = new JComboBox<QueuePolicy>(QueuePolicy.values());
    private final StyledButton btnSimulateAging = new StyledButton("+30m Aging Test", new Color(0xF3F4F6), new Color(0xE5E7EB), 6);

    // Multi-Threading Components
    private QueueMonitorThread queueMonitorThread;
    private final StyledButton btnToggleMonitor = new StyledButton("Live Monitor: ON (5s)", new Color(0xDC, 0xFC, 0xE7), new Color(0xBB, 0xF7, 0xD0), 6);
    private final StyledButton btnPolicySimulation = new StyledButton("Compare Policies (Threads)", new Color(0xFE, 0xF3, 0xC7), new Color(0xFD, 0xE6, 0x8A), 6);

    // Triage KPI Metric Labels
    private final JLabel lblMetricWaiting = new JLabel("0");
    private final JLabel lblMetricCritical = new JLabel("0");
    private final JLabel lblMetricPromoted = new JLabel("0");
    private final JLabel lblMetricNextUp = new JLabel("None");

    private final DefaultTableModel modelTriage = new DefaultTableModel(
            new String[]{"Rank", "Effective Category", "Patient", "Doctor", "Target Max", "Wait & Est. Range", "Anti-Starvation Aging", "Chief Complaint", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblTriage = new JTable(modelTriage);

    public DashboardFrame(String fullName, Role role) {
        super("MediQueue - Hospital Triage & Queue Management System");
        this.currentUser = fullName;
        this.currentRole = role;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);

        initUI();
        loadDoctors();
        refreshAppointments();
        refreshTriageQueue();
        refreshPendingAppointments();
        startLiveQueueMonitor();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopLiveQueueMonitor();
            }
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_LIGHT);

        // Top Header
        root.add(buildHeaderPanel(), BorderLayout.NORTH);

        // Main Tabbed Content
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(Color.WHITE);
        tabs.setForeground(PRIMARY_DARK);

        tabs.addTab("  1. Slot & Appointment Booking  ", buildBookingTab());
        tabs.addTab("  2. Manchester Triage Check-In & Priority Queue  ", buildTriageTab());

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x0F766E),
                        getWidth(), 0, new Color(0x042F2E)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle ambient glow
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(getWidth() - 180, -40, 200, 200);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(12, 22, 12, 22));

        // Brand & Title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);

        // Medical Cross Emblem Badge
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 36;
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, size, size);
                g2.setColor(PRIMARY);
                int crossThick = 5;
                int crossLen = 18;
                int cx = size / 2;
                int cy = size / 2;
                g2.fillRoundRect(cx - crossLen / 2, cy - crossThick / 2, crossLen, crossThick, 2, 2);
                g2.fillRoundRect(cx - crossThick / 2, cy - crossLen / 2, crossThick, crossLen, 2, 2);
                g2.dispose();
            }
        };
        logoBadge.setPreferredSize(new Dimension(36, 36));
        logoBadge.setOpaque(false);

        JLabel title = new JLabel("<html><b style='font-size:17px;color:white;'>MediQueue</b><br>"
                + "<span style='font-size:10px;color:#99F6E4;font-weight:bold;'>CLINICAL QUEUE &amp; APPOINTMENT MANAGEMENT SYSTEM</span></html>");

        brand.add(logoBadge);
        brand.add(title);

        // User info + Role Pill + MySQL Status + Logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        // User Profile Pill
        JPanel pnlUserBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlUserBadge.setOpaque(false);

        JLabel lblUserText = new JLabel("<html><span style='color:white;font-size:11px'>User: <b>" + currentUser + "</b> "
                + "(<span style='color:#5EEAD4;font-weight:bold;'>" + currentRole + "</span>)</span></html>");
        pnlUserBadge.add(lblUserText);

        // Database Status Pill
        JLabel dbStatus = new JLabel("<html><span style='color:#A7F3D0;font-size:11px;'>&#9679; MySQL Active</span></html>");

        // Modern Red Logout Button
        StyledButton btnLogout = new StyledButton("Log Out", new Color(0xDC2626), new Color(0xB91C1C), 8);
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnLogout.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnLogout.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        right.add(pnlUserBadge);
        right.add(dbStatus);
        right.add(btnLogout);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    // FEATURE 1: SLOT & APPOINTMENT BOOKING TAB
    // =========================================================================
    private JPanel buildBookingTab() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBackground(BG_LIGHT);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Top KPI Cards Row
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.add(buildKpiCard("Total Booked", lblMetricTotalAppts, new Color(0x0F766E), "Confirmed in system"));
        pnlKpis.add(buildKpiCard("Scheduled", lblMetricScheduled, new Color(0x2563EB), "Pre-booked slots"));
        pnlKpis.add(buildKpiCard("Walk-In", lblMetricWalkIns, new Color(0xD97706), "Direct clinic arrivals"));
        pnlKpis.add(buildKpiCard("Active Doctors", new JLabel(String.valueOf(bookingService.getDoctors().size())), new Color(0x059669), "Available for slots"));

        p.add(pnlKpis, BorderLayout.NORTH);

        // Center Content: Left Form + Right Table
        JPanel center = new JPanel(new BorderLayout(14, 14));
        center.setOpaque(false);

        // Left: Booking Form Card
        JPanel formCard = buildModernCard("New Appointment Booking", "Schedule a patient consultation slot with double-booking check");
        formCard.setPreferredSize(new Dimension(380, 0));

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);

        txtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        if (currentRole == Role.PATIENT) {
            txtPatientName.setText(currentUser);
        }
        txtPatientName.setAlphabetOnly(); // Alphabet characters only

        cmbSlots.removeAllItems();
        for (String slot : BookingService.STANDARD_SLOTS) {
            cmbSlots.addItem(slot);
        }

        styleComboBox(cmbDoctors);
        styleComboBox(cmbSlots);
        styleComboBox(cmbApptType);

        Dimension fieldDim = new Dimension(100, 34);
        txtPatientName.setPreferredSize(fieldDim);
        cmbDoctors.setPreferredSize(fieldDim);
        txtDate.setPreferredSize(fieldDim);
        cmbSlots.setPreferredSize(fieldDim);
        cmbApptType.setPreferredSize(fieldDim);
        btnBook.setPreferredSize(new Dimension(100, 38));

        // Quick Date Preset Buttons
        JPanel pnlDatePresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlDatePresets.setOpaque(false);
        StyledButton btnDateToday = new StyledButton("Today", new Color(0xCCFBF1), new Color(0x99F6E4), 6);
        btnDateToday.setForeground(PRIMARY_DARK);
        btnDateToday.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnDateToday.setBorder(new EmptyBorder(3, 8, 3, 8));
        btnDateToday.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                txtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            }
        });

        StyledButton btnDateTomorrow = new StyledButton("Tomorrow", new Color(0xF3F4F6), new Color(0xE5E7EB), 6);
        btnDateTomorrow.setForeground(TEXT_DARK);
        btnDateTomorrow.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnDateTomorrow.setBorder(new EmptyBorder(3, 8, 3, 8));
        btnDateTomorrow.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                txtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
            }
        });
        pnlDatePresets.add(btnDateToday);
        pnlDatePresets.add(btnDateTomorrow);

        // Date input container
        JPanel pnlDateRow = new JPanel(new BorderLayout(6, 0));
        pnlDateRow.setOpaque(false);
        pnlDateRow.add(txtDate, BorderLayout.CENTER);
        pnlDateRow.add(pnlDatePresets, BorderLayout.EAST);

        // Conflict check notice card
        JPanel pnlNotice = new JPanel(new BorderLayout(6, 0));
        pnlNotice.setBackground(new Color(0xF0, 0xFD, 0xFA));
        pnlNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x99, 0xF6, 0xE4), 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        JLabel lblNotice = new JLabel("<html><div style='width:280px;color:#0F766E;font-size:10px;'>"
                + "<b>Double-Booking Protection:</b> Duplicate doctor slots on the same date are automatically blocked.</div></html>");
        pnlNotice.add(lblNotice, BorderLayout.CENTER);

        int row = 0;
        c.gridy = row++; formBody.add(createFieldLabel("Patient Name", "Alphabet characters only"), c);
        c.gridy = row++; formBody.add(txtPatientName, c);

        c.insets = new Insets(4, 0, 3, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Select Doctor", "Choose physician"), c);
        c.gridy = row++; formBody.add(cmbDoctors, c);

        c.insets = new Insets(4, 0, 3, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Appointment Date", "YYYY-MM-DD format"), c);
        c.gridy = row++; formBody.add(pnlDateRow, c);

        c.insets = new Insets(4, 0, 3, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Time Slot", "15-minute consultation window"), c);
        c.gridy = row++; formBody.add(cmbSlots, c);

        c.insets = new Insets(4, 0, 3, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Booking Type", "Scheduled vs Walk-in"), c);
        c.gridy = row++; formBody.add(cmbApptType, c);

        c.insets = new Insets(8, 0, 4, 0);
        c.gridy = row++; formBody.add(pnlNotice, c);

        c.insets = new Insets(10, 0, 4, 0);
        c.gridy = row++; formBody.add(btnBook, c);

        formCard.add(formBody, BorderLayout.CENTER);

        btnBook.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doBookAppointment(); }
        });

        // Right: Appointments Table Card
        JPanel tableCard = buildModernCard("Booked Appointments Registry", "Real-time records of clinic consultations");

        JPanel tableHeaderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tableHeaderActions.setOpaque(false);

        StyledButton btnRefreshAppts = new StyledButton("Refresh", new Color(0xF3F4F6), new Color(0xE5E7EB), 6);
        btnRefreshAppts.setForeground(TEXT_DARK);
        btnRefreshAppts.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnRefreshAppts.setBorder(new EmptyBorder(5, 12, 5, 12));
        btnRefreshAppts.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { refreshAppointments(); }
        });
        tableHeaderActions.add(btnRefreshAppts);

        // Put action in card top
        JPanel cardHeaderPanel = (JPanel) tableCard.getComponent(0);
        cardHeaderPanel.add(tableHeaderActions, BorderLayout.EAST);

        // Style Table
        styleTable(tblAppointments);

        // Column widths
        tblAppointments.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblAppointments.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblAppointments.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblAppointments.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblAppointments.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblAppointments.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblAppointments.getColumnModel().getColumn(6).setPreferredWidth(95);

        // Custom renderer for Appointment Type and Status Pills
        tblAppointments.getColumnModel().getColumn(5).setCellRenderer(new PillCellRenderer());
        tblAppointments.getColumnModel().getColumn(6).setCellRenderer(new PillCellRenderer());

        JScrollPane sp = new JScrollPane(tblAppointments);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        sp.getViewport().setBackground(Color.WHITE);

        tableCard.add(sp, BorderLayout.CENTER);

        center.add(formCard, BorderLayout.WEST);
        center.add(tableCard, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private void doBookAppointment() {
        String patient = txtPatientName.getText().trim();
        Doctor doc = (Doctor) cmbDoctors.getSelectedItem();
        String date = txtDate.getText().trim();
        String slot = (String) cmbSlots.getSelectedItem();
        String type = (String) cmbApptType.getSelectedItem();

        if (patient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the patient's name.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            txtPatientName.requestFocusInWindow();
            return;
        }
        if (doc == null) {
            JOptionPane.showMessageDialog(this, "Please select a doctor.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (date.isEmpty() || !date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid date in YYYY-MM-DD format.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            txtDate.requestFocusInWindow();
            return;
        }

        String err = bookingService.bookAppointment(patient, doc.getId(), doc.getName(), date, slot, type);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Booking Notice / Double Booking", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "<html><b>Appointment successfully booked!</b><br><br>"
                        + "Patient: <b>" + patient + "</b><br>"
                        + "Doctor: <b>" + doc.getName() + "</b><br>"
                        + "Date: <b>" + date + "</b><br>"
                        + "Time Slot: <b>" + slot + "</b><br>"
                        + "Booking Type: <b>" + type + "</b></html>",
                "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

        if (currentRole != Role.PATIENT) {
            txtPatientName.setText("");
        }
        refreshAppointments();
        refreshPendingAppointments();
    }

    private void refreshAppointments() {
        modelAppointments.setRowCount(0);
        List<Appointment> list = bookingService.getAllAppointments();
        int scheduledCount = 0;
        int walkInCount = 0;

        for (Appointment a : list) {
            if ("WALK_IN".equalsIgnoreCase(a.getAppointmentType())) {
                walkInCount++;
            } else {
                scheduledCount++;
            }
            modelAppointments.addRow(new Object[]{
                    "#" + a.getId(),
                    a.getPatientName(),
                    a.getDoctorName(),
                    a.getAppointmentDate(),
                    a.getTimeSlot(),
                    a.getAppointmentType(),
                    a.getStatus()
            });
        }

        lblMetricTotalAppts.setText(String.valueOf(list.size()));
        lblMetricScheduled.setText(String.valueOf(scheduledCount));
        lblMetricWalkIns.setText(String.valueOf(walkInCount));
    }

    // =========================================================================
    // FEATURE 2: MANCHESTER TRIAGE CHECK-IN & PRIORITY QUEUE TAB
    // =========================================================================
    private JPanel buildTriageTab() {
        JPanel p = new JPanel(new BorderLayout(14, 14));
        p.setBackground(BG_LIGHT);
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Top KPI Cards Row
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.add(buildKpiCard("Patients Waiting", lblMetricWaiting, new Color(0x0F766E), "Active in queue"));
        pnlKpis.add(buildKpiCard("Critical (Cat 1-2)", lblMetricCritical, new Color(0xDC2626), "Immediate attention"));
        pnlKpis.add(buildKpiCard("Anti-Starvation Promoted", lblMetricPromoted, new Color(0xD97706), "Boosted by aging rule"));
        pnlKpis.add(buildKpiCard("Next In Line", lblMetricNextUp, new Color(0x059669), "Highest effective priority"));

        p.add(pnlKpis, BorderLayout.NORTH);

        // Center Content: Left Triage Form + Right Live Priority Queue
        JPanel center = new JPanel(new BorderLayout(14, 14));
        center.setOpaque(false);

        // Left: Manchester Triage Form Card
        JPanel formCard = buildModernCard("Manchester Triage Check-In", "Categorize patient urgency (Categories 1 to 5) into queue");
        formCard.setPreferredSize(new Dimension(390, 0));

        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);

        txtTriagePatient.setAlphabetOnly(); // Letters and spaces only

        styleComboBox(cmbPendingAppts);
        styleComboBox(cmbTriageDoctor);
        styleComboBox(cmbTriageCategory);

        Dimension fieldDim = new Dimension(100, 32);
        cmbPendingAppts.setPreferredSize(fieldDim);
        txtTriagePatient.setPreferredSize(fieldDim);
        cmbTriageDoctor.setPreferredSize(fieldDim);
        cmbTriageCategory.setPreferredSize(fieldDim);
        txtComplaint.setPreferredSize(fieldDim);
        btnCheckIn.setPreferredSize(new Dimension(100, 38));

        // Dynamic MTS Urgency Preview Banner
        pnlTriagePreview.setBackground(new Color(0xDC, 0xFC, 0xE7));
        pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x4A, 0xDE, 0x80), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        lblTriagePreviewText.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pnlTriagePreview.add(lblTriagePreviewText, BorderLayout.CENTER);
        updateTriagePreviewBanner((TriageCategory) cmbTriageCategory.getSelectedItem());

        cmbTriageCategory.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                updateTriagePreviewBanner((TriageCategory) cmbTriageCategory.getSelectedItem());
            }
        });

        int row = 0;
        c.gridy = row++; formBody.add(createFieldLabel("Fast Check-In from Booked Appt", "Optional auto-fill"), c);
        c.gridy = row++; formBody.add(cmbPendingAppts, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Patient Name", "Alphabet characters only"), c);
        c.gridy = row++; formBody.add(txtTriagePatient, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Assigned Doctor", "Physician on duty"), c);
        c.gridy = row++; formBody.add(cmbTriageDoctor, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Manchester Triage Category", "Urgency level (1 to 5)"), c);
        c.gridy = row++; formBody.add(cmbTriageCategory, c);

        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = row++; formBody.add(pnlTriagePreview, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; formBody.add(createFieldLabel("Chief Complaint / Symptoms", "Brief reason for visit"), c);
        c.gridy = row++; formBody.add(txtComplaint, c);

        c.insets = new Insets(8, 0, 4, 0);
        c.gridy = row++; formBody.add(btnCheckIn, c);

        formCard.add(formBody, BorderLayout.CENTER);

        // When pending appointment selected, prefill patient and doctor
        cmbPendingAppts.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int idx = cmbPendingAppts.getSelectedIndex();
                if (idx > 0) {
                    String selected = (String) cmbPendingAppts.getSelectedItem();
                    if (selected != null && selected.contains(" - ")) {
                        String[] parts = selected.split(" - ");
                        if (parts.length >= 2) {
                            String namePart = parts[1];
                            int parenIdx = namePart.indexOf(" (");
                            if (parenIdx != -1) {
                                String pName = namePart.substring(0, parenIdx).trim();
                                txtTriagePatient.setText(pName);
                                String docName = namePart.substring(parenIdx + 2, namePart.indexOf(")")).trim();
                                // Select matching doctor in cmbTriageDoctor
                                for (int i = 0; i < cmbTriageDoctor.getItemCount(); i++) {
                                    Doctor d = cmbTriageDoctor.getItemAt(i);
                                    if (d != null && d.getName().equalsIgnoreCase(docName)) {
                                        cmbTriageDoctor.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            } else {
                                txtTriagePatient.setText(namePart.trim());
                            }
                        }
                    }
                }
            }
        });

        btnCheckIn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doTriageCheckIn(); }
        });

        // Right: Live Triage Priority Queue Table Card
        JPanel queueCard = buildModernCard("Live Priority Queue (MTS)", "Dynamic priority ordering with 30-min Anti-Starvation aging promotion");

        // Action Toolbars: Header Controls (Policy + Refresh) & Primary Actions (Threads & Queue Operations)
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topControls.setOpaque(false);

        styleComboBox(cmbPolicy);
        cmbPolicy.setPreferredSize(new Dimension(210, 28));
        cmbPolicy.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cmbPolicy.setToolTipText("Select active queue ordering policy");
        cmbPolicy.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                currentPolicy = (QueuePolicy) cmbPolicy.getSelectedItem();
                refreshTriageQueue();
            }
        });

        StyledButton btnRefreshQueue = new StyledButton("Refresh", new Color(0xF3F4F6), new Color(0xE5E7EB), 6);
        btnRefreshQueue.setForeground(TEXT_DARK);
        btnRefreshQueue.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnRefreshQueue.setBorder(new EmptyBorder(5, 12, 5, 12));
        btnRefreshQueue.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { refreshTriageQueue(); }
        });

        topControls.add(cmbPolicy);
        topControls.add(btnRefreshQueue);

        JPanel queueCardHeader = (JPanel) queueCard.getComponent(0);
        queueCardHeader.add(topControls, BorderLayout.EAST);

        btnSimulateAging.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnSimulateAging.setBorder(new EmptyBorder(5, 10, 5, 10));
        btnSimulateAging.setToolTipText("Fast-forward simulated wait by 30 mins to demonstrate anti-starvation aging");
        btnSimulateAging.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (simulatedAgingMinutes == 0) {
                    simulatedAgingMinutes = 30;
                    btnSimulateAging.setText("+30m Aging Active");
                    btnSimulateAging.setBackground(new Color(0xFE, 0xF3, 0xC7));
                    btnSimulateAging.setForeground(new Color(0x92, 0x40, 0x0E));
                } else if (simulatedAgingMinutes == 30) {
                    simulatedAgingMinutes = 60;
                    btnSimulateAging.setText("+60m Aging Active");
                    btnSimulateAging.setBackground(new Color(0xFE, 0xE2, 0xE2));
                    btnSimulateAging.setForeground(new Color(0x99, 0x1B, 0x1B));
                } else {
                    simulatedAgingMinutes = 0;
                    btnSimulateAging.setText("+30m Aging Test");
                    btnSimulateAging.setBackground(new Color(0xF3, 0xF4, 0xF6));
                    btnSimulateAging.setForeground(TEXT_DARK);
                }
                refreshTriageQueue();
            }
        });

        btnCallNext.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnCallNext.setBorder(new EmptyBorder(5, 12, 5, 12));
        btnCallNext.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doCallNext(); }
        });

        btnToggleMonitor.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnToggleMonitor.setBorder(new EmptyBorder(5, 10, 5, 10));
        btnToggleMonitor.setToolTipText("Toggle background daemon thread that auto-refreshes queue wait times every 5s");
        btnToggleMonitor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (queueMonitorThread != null && queueMonitorThread.isMonitoring()) {
                    queueMonitorThread.pauseMonitoring();
                } else if (queueMonitorThread != null && queueMonitorThread.isPaused()) {
                    queueMonitorThread.resumeMonitoring();
                } else {
                    startLiveQueueMonitor();
                }
                updateMonitorStatusUI();
            }
        });

        btnPolicySimulation.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnPolicySimulation.setForeground(new Color(0x92, 0x40, 0x0E));
        btnPolicySimulation.setBorder(new EmptyBorder(5, 10, 5, 10));
        btnPolicySimulation.setToolTipText("Run 3 concurrent worker threads comparing FCFS, Strict Priority, and Priority with Aging");
        btnPolicySimulation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SimulationDialog(DashboardFrame.this).setVisible(true);
            }
        });

        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        actionToolbar.setOpaque(false);
        actionToolbar.add(btnToggleMonitor);
        actionToolbar.add(btnPolicySimulation);
        actionToolbar.add(btnSimulateAging);
        actionToolbar.add(btnCallNext);

        JPanel northContainer = new JPanel(new BorderLayout(0, 6));
        northContainer.setOpaque(false);
        northContainer.add(queueCardHeader, BorderLayout.NORTH);
        northContainer.add(actionToolbar, BorderLayout.SOUTH);
        queueCard.add(northContainer, BorderLayout.NORTH);

        // Style Queue Table
        styleTable(tblTriage);

        // Column widths
        tblTriage.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblTriage.getColumnModel().getColumn(1).setPreferredWidth(155);
        tblTriage.getColumnModel().getColumn(2).setPreferredWidth(105);
        tblTriage.getColumnModel().getColumn(3).setPreferredWidth(105);
        tblTriage.getColumnModel().getColumn(4).setPreferredWidth(85);
        tblTriage.getColumnModel().getColumn(5).setPreferredWidth(115);
        tblTriage.getColumnModel().getColumn(6).setPreferredWidth(140);
        tblTriage.getColumnModel().getColumn(7).setPreferredWidth(125);
        tblTriage.getColumnModel().getColumn(8).setPreferredWidth(80);

        // Custom renderers
        tblTriage.getColumnModel().getColumn(0).setCellRenderer(new RankCellRenderer());
        tblTriage.getColumnModel().getColumn(1).setCellRenderer(new TriageCategoryBadgeRenderer());
        tblTriage.getColumnModel().getColumn(4).setCellRenderer(new CenteredCellRenderer());
        tblTriage.getColumnModel().getColumn(5).setCellRenderer(new CenteredCellRenderer());
        tblTriage.getColumnModel().getColumn(6).setCellRenderer(new AgingBadgeRenderer());
        tblTriage.getColumnModel().getColumn(8).setCellRenderer(new PillCellRenderer());

        JScrollPane sp = new JScrollPane(tblTriage);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        sp.getViewport().setBackground(Color.WHITE);

        // Anti-Starvation Fairness Rule Info Strip at Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        footer.setOpaque(false);
        lblQueueStats.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblQueueStats.setForeground(TEXT_DARK);
        footer.add(lblQueueStats);

        queueCard.add(sp, BorderLayout.CENTER);
        queueCard.add(footer, BorderLayout.SOUTH);

        center.add(formCard, BorderLayout.WEST);
        center.add(queueCard, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private void updateTriagePreviewBanner(TriageCategory cat) {
        if (cat == null) return;
        int level = cat.getLevel();
        String waitText = cat.getTargetWaitMinutes() == 0 ? "Immediate Attention (0 min wait)" : cat.getTargetWaitMinutes() + " minutes max wait";

        if (level == 1) {
            pnlTriagePreview.setBackground(new Color(0xFE, 0xE2, 0xE2));
            pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF8, 0x71, 0x71), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            lblTriagePreviewText.setText("<html><div style='width:280px;'><b style='color:#991B1B;'>CATEGORY 1: IMMEDIATE / RESUSCITATION</b><br>"
                    + "<span style='color:#7F1D1D;'>Urgency: <b>" + waitText + "</b> • Top queue priority</span></div></html>");
        } else if (level == 2) {
            pnlTriagePreview.setBackground(new Color(0xFF, 0xED, 0xD5));
            pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xFB, 0x92, 0x3C), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            lblTriagePreviewText.setText("<html><div style='width:280px;'><b style='color:#9A3412;'>CATEGORY 2: VERY URGENT</b><br>"
                    + "<span style='color:#7C2D12;'>Urgency: <b>" + waitText + "</b> • High clinical priority</span></div></html>");
        } else if (level == 3) {
            pnlTriagePreview.setBackground(new Color(0xFE, 0xF9, 0xC3));
            pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xFA, 0xCC, 0x15), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            lblTriagePreviewText.setText("<html><div style='width:280px;'><b style='color:#854D0E;'>CATEGORY 3: URGENT</b><br>"
                    + "<span style='color:#713F12;'>Urgency: <b>" + waitText + "</b> • Moderate urgency queue</span></div></html>");
        } else if (level == 4) {
            pnlTriagePreview.setBackground(new Color(0xDC, 0xFC, 0xE7));
            pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x4A, 0xDE, 0x80), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            lblTriagePreviewText.setText("<html><div style='width:280px;'><b style='color:#166534;'>CATEGORY 4: STANDARD</b><br>"
                    + "<span style='color:#14532D;'>Urgency: <b>" + waitText + "</b> • Standard outpatient priority</span></div></html>");
        } else {
            pnlTriagePreview.setBackground(new Color(0xDB, 0xEA, 0xFE));
            pnlTriagePreview.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x60, 0xA5, 0xFA), 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            lblTriagePreviewText.setText("<html><div style='width:280px;'><b style='color:#1E40AF;'>CATEGORY 5: NON-URGENT</b><br>"
                    + "<span style='color:#1E3A8A;'>Urgency: <b>" + waitText + "</b> • Routine consultation</span></div></html>");
        }
    }

    private void doTriageCheckIn() {
        String patient = txtTriagePatient.getText().trim();
        Doctor doc = (Doctor) cmbTriageDoctor.getSelectedItem();
        TriageCategory cat = (TriageCategory) cmbTriageCategory.getSelectedItem();
        String complaint = txtComplaint.getText().trim();

        if (patient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter patient name.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            txtTriagePatient.requestFocusInWindow();
            return;
        }
        if (doc == null) {
            JOptionPane.showMessageDialog(this, "Please select a doctor.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer apptId = null;
        int idx = cmbPendingAppts.getSelectedIndex();
        if (idx > 0) {
            String sel = (String) cmbPendingAppts.getSelectedItem();
            if (sel != null && sel.startsWith("#")) {
                try {
                    apptId = Integer.parseInt(sel.substring(1, sel.indexOf(" ")));
                } catch (Exception ignored) { }
            }
        }

        String err = triageService.checkInPatient(apptId, patient, doc.getId(), doc.getName(), cat, complaint);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Check-In Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "<html><b>Patient successfully triaged into live queue!</b><br><br>"
                        + "Patient: <b>" + patient + "</b><br>"
                        + "Assigned Doctor: <b>" + doc.getName() + "</b><br>"
                        + "Manchester Urgency: <b>" + cat.getDescription() + "</b><br>"
                        + "Target Max Wait: <b>" + (cat.getTargetWaitMinutes() == 0 ? "Immediate" : cat.getTargetWaitMinutes() + " mins") + "</b></html>",
                "Triage Recorded", JOptionPane.INFORMATION_MESSAGE);

        txtTriagePatient.setText("");
        txtComplaint.setText("");
        cmbPendingAppts.setSelectedIndex(0);
        refreshTriageQueue();
        refreshPendingAppointments();
        refreshAppointments();
    }

    private void refreshTriageQueue() {
        modelTriage.setRowCount(0);
        List<TriageEntry> queue = triageService.getActiveTriageQueue(currentPolicy, simulatedAgingMinutes);
        int rank = 1;
        int urgentCount = 0;
        int promotedCount = 0;

        for (int i = 0; i < queue.size(); i++) {
            TriageEntry te = queue.get(i);
            TriageCategory effCat = te.getEffectiveCategory(simulatedAgingMinutes);
            long waitMins = te.getWaitMinutes(simulatedAgingMinutes);
            int boost = te.getAgingBoost(simulatedAgingMinutes);
            boolean isOverdue = te.isOverdue(simulatedAgingMinutes);

            if (effCat.getLevel() <= 2) {
                urgentCount++;
            }
            if (boost > 0) {
                promotedCount++;
            }

            // Estimated wait range based on queue position
            String estWaitRange;
            if (i == 0) {
                estWaitRange = "Now / Next";
            } else {
                estWaitRange = (i * 10) + "–" + (i * 15) + " mins";
            }

            // Anti-Starvation Aging label
            String agingLabel;
            if (boost > 0) {
                agingLabel = "▲ +" + boost + " Level Promoted (Aging)";
            } else if (isOverdue) {
                agingLabel = "! Overdue Target";
            } else {
                agingLabel = "Normal Priority";
            }

            modelTriage.addRow(new Object[]{
                    "#" + (rank++),
                    effCat.getDescription(),
                    te.getPatientName(),
                    te.getDoctorName(),
                    te.getCategory().getTargetWaitMinutes() == 0 ? "Immediate" : te.getCategory().getTargetWaitMinutes() + " mins",
                    waitMins + "m (Est: " + estWaitRange + ")",
                    agingLabel,
                    te.getChiefComplaint() != null && !te.getChiefComplaint().isEmpty() ? te.getChiefComplaint() : "None stated",
                    te.getStatus()
            });
        }

        lblMetricWaiting.setText(String.valueOf(queue.size()));
        lblMetricCritical.setText(String.valueOf(urgentCount));
        lblMetricPromoted.setText(String.valueOf(promotedCount));

        if (!queue.isEmpty()) {
            TriageEntry top = queue.get(0);
            lblMetricNextUp.setText(top.getPatientName());
        } else {
            lblMetricNextUp.setText("Queue Empty");
        }

        String agingInfo = (currentPolicy == QueuePolicy.PRIORITY_WITH_AGING)
                ? "Active (+1 level per 30m wait)" : "Inactive (" + currentPolicy + ")";

        String threadInfo = (queueMonitorThread != null && queueMonitorThread.isMonitoring())
                ? "Live Monitor: Running (Tick #" + queueMonitorThread.getTickCount() + ")"
                : "Live Monitor: Paused";

        lblQueueStats.setText("Policy: " + currentPolicy + " | Anti-Starvation Rule: " + agingInfo
                + " | " + queue.size() + " Waiting (" + promotedCount + " boosted) | " + threadInfo);
    }

    /**
     * Starts the background daemon thread that periodically refreshes the queue
     * and evaluates Anti-Starvation aging in real-time.
     */
    public synchronized void startLiveQueueMonitor() {
        if (queueMonitorThread != null && queueMonitorThread.isAlive()) {
            queueMonitorThread.stopMonitoring();
        }
        queueMonitorThread = new QueueMonitorThread(5000, new Runnable() {
            @Override
            public void run() {
                refreshTriageQueue();
                updateMonitorStatusUI();
            }
        });
        queueMonitorThread.start();
        updateMonitorStatusUI();
    }

    /**
     * Stops the background monitor thread gracefully.
     */
    public synchronized void stopLiveQueueMonitor() {
        if (queueMonitorThread != null) {
            queueMonitorThread.stopMonitoring();
        }
        updateMonitorStatusUI();
    }

    private void updateMonitorStatusUI() {
        if (queueMonitorThread != null && queueMonitorThread.isMonitoring()) {
            btnToggleMonitor.setText("Live Monitor: ON (5s)");
            btnToggleMonitor.setBackground(new Color(0xDC, 0xFC, 0xE7));
            btnToggleMonitor.setForeground(new Color(0x16, 0x65, 0x34));
        } else {
            btnToggleMonitor.setText("Live Monitor: PAUSED");
            btnToggleMonitor.setBackground(new Color(0xFE, 0xE2, 0xE2));
            btnToggleMonitor.setForeground(new Color(0x99, 0x1B, 0x1B));
        }
    }

    private void doCallNext() {
        List<TriageEntry> queue = triageService.getActiveTriageQueue(currentPolicy, simulatedAgingMinutes);
        if (queue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The queue is currently empty. No patients waiting.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        TriageEntry top = queue.get(0);
        TriageCategory effCat = top.getEffectiveCategory(simulatedAgingMinutes);
        int boost = top.getAgingBoost(simulatedAgingMinutes);
        String agingNote = boost > 0 ? "<br><span style='color:#D97706;'><b>▲ Promoted +" + boost + " levels by Anti-Starvation Aging Rule</b></span>" : "";

        int choice = JOptionPane.showConfirmDialog(this,
                "<html><b>Calling Next Patient:</b><br><br>"
                        + "Patient: <b style='color:#0F766E;font-size:14px;'>" + top.getPatientName() + "</b><br>"
                        + "Effective Priority: <b>" + effCat.getDescription() + "</b>" + agingNote + "<br>"
                        + "Doctor: <b>" + top.getDoctorName() + "</b><br>"
                        + "Wait Time: <b>" + top.getWaitMinutes(simulatedAgingMinutes) + " mins</b><br>"
                        + "Complaint: <i>" + (top.getChiefComplaint() != null ? top.getChiefComplaint() : "None") + "</i><br><br>"
                        + "Mark consultation as completed / served?</html>",
                "Call Patient", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            triageService.markServed(top.getId());
            refreshTriageQueue();
        }
    }

    private void refreshPendingAppointments() {
        cmbPendingAppts.removeAllItems();
        cmbPendingAppts.addItem("-- Select Booked Appt to Fast Check-In --");
        List<Appointment> pending = triageService.getPendingAppointments();
        for (Appointment a : pending) {
            cmbPendingAppts.addItem("#" + a.getId() + " - " + a.getPatientName() + " (" + a.getDoctorName() + ")");
        }
    }

    private void loadDoctors() {
        cmbDoctors.removeAllItems();
        cmbTriageDoctor.removeAllItems();
        List<Doctor> doctors = bookingService.getDoctors();
        for (Doctor d : doctors) {
            cmbDoctors.addItem(d);
            cmbTriageDoctor.addItem(d);
        }
    }

    // =========================================================================
    // UI HELPER & STYLING METHODS
    // =========================================================================
    private JPanel buildModernCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_DARK);

        JLabel lblSub = new JLabel("<html><span style='color:#6B7280;font-size:11px'>" + subtitle + "</span></html>");

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        header.add(titlePanel, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildKpiCard(String label, JLabel valueLabel, Color accentColor, String desc) {
        JPanel card = new JPanel(new BorderLayout(6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblTitle.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblDesc.setForeground(new Color(0x9CA3AF));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(lblDesc, BorderLayout.SOUTH);
        return card;
    }

    private JLabel createFieldLabel(String title, String hint) {
        JLabel lbl = new JLabel("<html><b>" + title + "</b> <span style='color:#9CA3AF;font-size:10px;'>(" + hint + ")</span></html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    private <T> void styleComboBox(JComboBox<T> cb) {
        cb.setBackground(Color.WHITE);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setFocusable(false);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    lbl.setBackground(new Color(0xCCFBF1));
                    lbl.setForeground(PRIMARY);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(TEXT_DARK);
                }
                return lbl;
            }
        });
    }

    private void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(0xCCFBF1));
        table.setSelectionForeground(PRIMARY_DARK);

        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setBackground(new Color(0xF9FAFB));
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Default alternating row renderer for general columns
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                    c.setForeground(TEXT_DARK);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
        }
    }

    // =========================================================================
    // CUSTOM TABLE CELL RENDERERS
    // =========================================================================
    private static class CenteredCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                lbl.setForeground(TEXT_DARK);
            }
            return lbl;
        }
    }

    private static class RankCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (!isSelected) {
                if (row == 0) {
                    lbl.setBackground(new Color(0xDC, 0xFC, 0xE7));
                    lbl.setForeground(new Color(0x16, 0x65, 0x34));
                    lbl.setText(value + " (NEXT)");
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                    lbl.setForeground(TEXT_DARK);
                }
            }
            return lbl;
        }
    }

    private static class AgingBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            String val = String.valueOf(value);

            if (!isSelected) {
                lbl.setOpaque(true);
                if (val.contains("Promoted")) {
                    lbl.setBackground(new Color(0xFE, 0xF3, 0xC7));
                    lbl.setForeground(new Color(0x92, 0x40, 0x0E));
                } else if (val.contains("Overdue")) {
                    lbl.setBackground(new Color(0xFE, 0xE2, 0xE2));
                    lbl.setForeground(new Color(0x99, 0x1B, 0x1B));
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                    lbl.setForeground(new Color(0x6B, 0x72, 0x80));
                }
            }
            return lbl;
        }
    }

    private static class PillCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));

            String text = String.valueOf(value).toUpperCase();
            if (!isSelected) {
                lbl.setOpaque(true);
                if (text.contains("SCHEDULED") || text.contains("BOOKED")) {
                    lbl.setBackground(new Color(0xDB, 0xEA, 0xFE));
                    lbl.setForeground(new Color(0x1E, 0x40, 0xAF));
                } else if (text.contains("WALK_IN") || text.contains("WAITING")) {
                    lbl.setBackground(new Color(0xFF, 0xED, 0xD5));
                    lbl.setForeground(new Color(0x9A, 0x34, 0x12));
                } else if (text.contains("SERVED") || text.contains("COMPLETED")) {
                    lbl.setBackground(new Color(0xDC, 0xFC, 0xE7));
                    lbl.setForeground(new Color(0x16, 0x65, 0x34));
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                    lbl.setForeground(TEXT_DARK);
                }
            }
            return lbl;
        }
    }

    private static class TriageCategoryBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String val = String.valueOf(value);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));

            if (!isSelected) {
                if (val.contains("Category 1") || val.contains("Immediate")) {
                    lbl.setBackground(new Color(0xFE, 0xE2, 0xE2));
                    lbl.setForeground(new Color(0x99, 0x1B, 0x1B));
                    lbl.setText(val);
                } else if (val.contains("Category 2") || val.contains("Very Urgent")) {
                    lbl.setBackground(new Color(0xFF, 0xED, 0xD5));
                    lbl.setForeground(new Color(0x9A, 0x34, 0x12));
                    lbl.setText(val);
                } else if (val.contains("Category 3") || val.contains("Urgent")) {
                    lbl.setBackground(new Color(0xFE, 0xF9, 0xC3));
                    lbl.setForeground(new Color(0x85, 0x4D, 0x0E));
                    lbl.setText(val);
                } else if (val.contains("Category 4") || val.contains("Standard")) {
                    lbl.setBackground(new Color(0xDC, 0xFC, 0xE7));
                    lbl.setForeground(new Color(0x16, 0x65, 0x34));
                    lbl.setText(val);
                } else {
                    lbl.setBackground(new Color(0xDB, 0xEA, 0xFE));
                    lbl.setForeground(new Color(0x1E, 0x40, 0xAF));
                    lbl.setText(val);
                }
            }
            return lbl;
        }
    }
}
