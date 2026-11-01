package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
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

/**
 * MediQueue Main Dashboard.
 * Implements 40% Project Milestone:
 * 1. Slot & Appointment Booking with Double-Booking check (Feature 1)
 * 2. Manchester Triage System (MTS) Check-In & Priority Queue (Feature 2)
 */
public class DashboardFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color BG_LIGHT = new Color(0xF3F4F6);
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);

    private final String currentUser;
    private final Role currentRole;

    private final BookingService bookingService = new BookingService();
    private final TriageService triageService = new TriageService();

    // Booking Tab Components
    private final JTextField txtPatientName = new JTextField();
    private final JComboBox<Doctor> cmbDoctors = new JComboBox<Doctor>();
    private final JTextField txtDate = new JTextField();
    private final JComboBox<String> cmbSlots = new JComboBox<String>();
    private final JComboBox<String> cmbApptType = new JComboBox<String>(new String[]{"SCHEDULED", "WALK_IN"});
    private final JButton btnBook = new JButton("Confirm Booking");
    private final DefaultTableModel modelAppointments = new DefaultTableModel(
            new String[]{"ID", "Patient", "Doctor", "Date", "Slot", "Type", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblAppointments = new JTable(modelAppointments);

    // Triage Tab Components
    private final JComboBox<String> cmbPendingAppts = new JComboBox<String>();
    private final JTextField txtTriagePatient = new JTextField();
    private final JComboBox<Doctor> cmbTriageDoctor = new JComboBox<Doctor>();
    private final JComboBox<TriageCategory> cmbTriageCategory = new JComboBox<TriageCategory>(TriageCategory.values());
    private final JTextField txtComplaint = new JTextField();
    private final JButton btnCheckIn = new JButton("Complete Triage Check-In");
    private final JButton btnCallNext = new JButton("Call Next Patient / Mark Served");
    private final JLabel lblQueueStats = new JLabel("Queue Status: Loading...");
    private final DefaultTableModel modelTriage = new DefaultTableModel(
            new String[]{"Rank", "Category", "Patient", "Doctor", "Target Max Wait", "Chief Complaint", "Check-in Time", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblTriage = new JTable(modelTriage);

    public DashboardFrame(String fullName, Role role) {
        super("MediQueue - Hospital Triage & Queue Management System");
        this.currentUser = fullName;
        this.currentRole = role;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        initUI();
        loadDoctors();
        refreshAppointments();
        refreshTriageQueue();
        refreshPendingAppointments();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_LIGHT);

        // Top Header
        root.add(buildHeaderPanel(), BorderLayout.NORTH);

        // Main Tabbed Content
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("  1. Slot & Appointment Booking  ", buildBookingTab());
        tabs.addTab("  2. Manchester Triage Check-In & Queue  ", buildTriageTab());

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Brand & Title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel("+");
        logo.setFont(new Font("SansSerif", Font.BOLD, 28));
        logo.setForeground(Color.WHITE);
        JLabel title = new JLabel("<html><b style='font-size:16px;color:white;'>MediQueue</b><br>"
                + "<span style='font-size:11px;color:#CCFBF1;'>Hospital Triage &amp; Appointment Queue System</span></html>");
        brand.add(logo);
        brand.add(title);

        // User info + Progress + Logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);

        JLabel userBadge = new JLabel("<html><div style='text-align:right;color:white;'>"
                + "User: <b>" + currentUser + "</b> &nbsp;|&nbsp; Role: <b>" + currentRole + "</b>"
                + "<br><span style='font-size:10px;color:#A7F3D0;'>● MySQL Connected (mediqueue_db)</span></div></html>");

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setBackground(new Color(0xB91C1C));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        right.add(userBadge);
        right.add(btnLogout);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    // FEATURE 1: SLOT & APPOINTMENT BOOKING TAB
    // =========================================================================
    private JPanel buildBookingTab() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG_LIGHT);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: Booking Form
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(16, 16, 16, 16)));
        formCard.setPreferredSize(new Dimension(360, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);

        JLabel lblTitle = new JLabel("Book Appointment");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel lblDesc = new JLabel("<html><span style='color:#6B7280;font-size:11px'>Select doctor, date &amp; slot. Checks for double-booking.</span></html>");

        txtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        if (currentRole == Role.PATIENT) {
            txtPatientName.setText(currentUser);
        }

        for (String slot : BookingService.STANDARD_SLOTS) {
            cmbSlots.addItem(slot);
        }

        btnBook.setBackground(PRIMARY);
        btnBook.setForeground(Color.WHITE);
        btnBook.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnBook.setFocusPainted(false);
        btnBook.setBorder(new EmptyBorder(9, 0, 9, 0));
        btnBook.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        int row = 0;
        c.gridy = row++; formCard.add(lblTitle, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0); formCard.add(lblDesc, c);
        c.insets = new Insets(3, 0, 3, 0);

        c.gridy = row++; formCard.add(new JLabel("Patient Name"), c);
        c.gridy = row++; formCard.add(txtPatientName, c);

        c.gridy = row++; formCard.add(new JLabel("Select Doctor"), c);
        c.gridy = row++; formCard.add(cmbDoctors, c);

        c.gridy = row++; formCard.add(new JLabel("Date (YYYY-MM-DD)"), c);
        c.gridy = row++; formCard.add(txtDate, c);

        c.gridy = row++; formCard.add(new JLabel("Time Slot"), c);
        c.gridy = row++; formCard.add(cmbSlots, c);

        c.gridy = row++; formCard.add(new JLabel("Booking Type"), c);
        c.gridy = row++; formCard.add(cmbApptType, c);

        c.gridy = row++; c.insets = new Insets(12, 0, 4, 0); formCard.add(btnBook, c);

        btnBook.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doBookAppointment(); }
        });

        // Right: Appointments Table
        JPanel tableCard = new JPanel(new BorderLayout(8, 8));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(12, 12, 12, 12)));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        JLabel lblTblTitle = new JLabel("Booked Appointments");
        lblTblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        JButton btnRefreshAppts = new JButton("Refresh");
        btnRefreshAppts.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { refreshAppointments(); }
        });
        tableHeader.add(lblTblTitle, BorderLayout.WEST);
        tableHeader.add(btnRefreshAppts, BorderLayout.EAST);

        tblAppointments.setRowHeight(24);
        tblAppointments.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        JScrollPane sp = new JScrollPane(tblAppointments);

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        p.add(formCard, BorderLayout.WEST);
        p.add(tableCard, BorderLayout.CENTER);
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
            return;
        }
        if (doc == null) {
            JOptionPane.showMessageDialog(this, "Please select a doctor.", "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String err = bookingService.bookAppointment(patient, doc.getId(), doc.getName(), date, slot, type);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Booking Notice / Double Booking", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Appointment successfully booked!\n\nPatient: " + patient + "\nDoctor: " + doc.getName()
                        + "\nDate: " + date + "\nSlot: " + slot,
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
        for (Appointment a : list) {
            modelAppointments.addRow(new Object[]{
                    a.getId(),
                    a.getPatientName(),
                    a.getDoctorName(),
                    a.getAppointmentDate(),
                    a.getTimeSlot(),
                    a.getAppointmentType(),
                    a.getStatus()
            });
        }
    }

    // =========================================================================
    // FEATURE 2: MANCHESTER TRIAGE CHECK-IN & PRIORITY QUEUE TAB
    // =========================================================================
    private JPanel buildTriageTab() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG_LIGHT);
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: Triage Form
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(16, 16, 16, 16)));
        formCard.setPreferredSize(new Dimension(380, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);

        JLabel lblTitle = new JLabel("Manchester Triage Check-In");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel lblDesc = new JLabel("<html><span style='color:#6B7280;font-size:11px'>"
                + "Categorize patients by urgency (Categories 1–5). Category 1 jumps to the front.</span></html>");

        btnCheckIn.setBackground(PRIMARY);
        btnCheckIn.setForeground(Color.WHITE);
        btnCheckIn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCheckIn.setFocusPainted(false);
        btnCheckIn.setBorder(new EmptyBorder(9, 0, 9, 0));
        btnCheckIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        int row = 0;
        c.gridy = row++; formCard.add(lblTitle, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0); formCard.add(lblDesc, c);
        c.insets = new Insets(2, 0, 2, 0);

        c.gridy = row++; formCard.add(new JLabel("Fast Check-In from Booked Appt (Optional)"), c);
        c.gridy = row++; formCard.add(cmbPendingAppts, c);

        c.gridy = row++; formCard.add(new JLabel("Patient Name"), c);
        c.gridy = row++; formCard.add(txtTriagePatient, c);

        c.gridy = row++; formCard.add(new JLabel("Doctor"), c);
        c.gridy = row++; formCard.add(cmbTriageDoctor, c);

        c.gridy = row++; formCard.add(new JLabel("Manchester Triage Category (1–5)"), c);
        c.gridy = row++; formCard.add(cmbTriageCategory, c);

        c.gridy = row++; formCard.add(new JLabel("Chief Complaint / Symptoms"), c);
        c.gridy = row++; formCard.add(txtComplaint, c);

        c.gridy = row++; c.insets = new Insets(10, 0, 4, 0); formCard.add(btnCheckIn, c);

        // When pending appointment selected, prefill patient and doctor
        cmbPendingAppts.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int idx = cmbPendingAppts.getSelectedIndex();
                if (idx > 0) {
                    String selected = (String) cmbPendingAppts.getSelectedItem();
                    if (selected != null && selected.contains(" - ")) {
                        String[] parts = selected.split(" - ");
                        if (parts.length >= 2) {
                            txtTriagePatient.setText(parts[1]);
                        }
                    }
                }
            }
        });

        btnCheckIn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doTriageCheckIn(); }
        });

        // Right: Live Triage Priority Queue Table
        JPanel queueCard = new JPanel(new BorderLayout(8, 8));
        queueCard.setBackground(Color.WHITE);
        queueCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(12, 12, 12, 12)));

        JPanel queueHeader = new JPanel(new BorderLayout());
        queueHeader.setOpaque(false);
        JLabel lblQueueTitle = new JLabel("Live Priority Queue (Manchester Triage System)");
        lblQueueTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel queueActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        queueActions.setOpaque(false);

        btnCallNext.setBackground(new Color(0x11, 0x5E, 0x59));
        btnCallNext.setForeground(Color.WHITE);
        btnCallNext.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCallNext.setFocusPainted(false);
        btnCallNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCallNext.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doCallNext(); }
        });

        JButton btnRefreshQueue = new JButton("Refresh Queue");
        btnRefreshQueue.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { refreshTriageQueue(); }
        });

        queueActions.add(btnCallNext);
        queueActions.add(btnRefreshQueue);

        queueHeader.add(lblQueueTitle, BorderLayout.WEST);
        queueHeader.add(queueActions, BorderLayout.EAST);

        tblTriage.setRowHeight(26);
        tblTriage.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // Custom renderer to color-code the Category column based on Manchester Triage colors
        tblTriage.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = String.valueOf(value);
                c.setOpaque(true);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(new Font("SansSerif", Font.BOLD, 11));
                if (val.contains("Category 1") || val.contains("Immediate")) {
                    c.setBackground(new Color(0xFE, 0xE2, 0xE2));
                    c.setForeground(new Color(0x99, 0x1B, 0x1B));
                } else if (val.contains("Category 2") || val.contains("Very Urgent")) {
                    c.setBackground(new Color(0xFF, 0xED, 0xD5));
                    c.setForeground(new Color(0x9A, 0x34, 0x12));
                } else if (val.contains("Category 3") || val.contains("Urgent")) {
                    c.setBackground(new Color(0xFE, 0xF9, 0xC3));
                    c.setForeground(new Color(0x85, 0x4D, 0x0E));
                } else if (val.contains("Category 4") || val.contains("Standard")) {
                    c.setBackground(new Color(0xDC, 0xFC, 0xE7));
                    c.setForeground(new Color(0x16, 0x65, 0x34));
                } else {
                    c.setBackground(new Color(0xDB, 0xEA, 0xFE));
                    c.setForeground(new Color(0x1E, 0x40, 0xAF));
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(tblTriage);

        lblQueueStats.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblQueueStats.setForeground(new Color(0x37, 0x41, 0x51));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setOpaque(false);
        footer.add(lblQueueStats);

        queueCard.add(queueHeader, BorderLayout.NORTH);
        queueCard.add(sp, BorderLayout.CENTER);
        queueCard.add(footer, BorderLayout.SOUTH);

        p.add(formCard, BorderLayout.WEST);
        p.add(queueCard, BorderLayout.CENTER);
        return p;
    }

    private void doTriageCheckIn() {
        String patient = txtTriagePatient.getText().trim();
        Doctor doc = (Doctor) cmbTriageDoctor.getSelectedItem();
        TriageCategory cat = (TriageCategory) cmbTriageCategory.getSelectedItem();
        String complaint = txtComplaint.getText().trim();

        if (patient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter patient name.", "Missing Information", JOptionPane.WARNING_MESSAGE);
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
                "Patient successfully triaged into queue!\n\nPatient: " + patient + "\nDoctor: " + doc.getName()
                        + "\nTriage Category: " + cat.getDescription()
                        + "\nTarget Max Wait: " + (cat.getTargetWaitMinutes() == 0 ? "Immediate" : cat.getTargetWaitMinutes() + " mins"),
                "Triage Recorded", JOptionPane.INFORMATION_MESSAGE);

        txtTriagePatient.setText("");
        txtComplaint.setText("");
        refreshTriageQueue();
        refreshPendingAppointments();
        refreshAppointments();
    }

    private void refreshTriageQueue() {
        modelTriage.setRowCount(0);
        List<TriageEntry> queue = triageService.getActiveTriageQueue();
        int rank = 1;
        int urgentCount = 0;
        int standardCount = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

        for (TriageEntry te : queue) {
            if (te.getCategory().getLevel() <= 2) {
                urgentCount++;
            } else {
                standardCount++;
            }

            modelTriage.addRow(new Object[]{
                    "#" + (rank++),
                    te.getCategory().getDescription(),
                    te.getPatientName(),
                    te.getDoctorName(),
                    te.getCategory().getTargetWaitMinutes() == 0 ? "Immediate" : te.getCategory().getTargetWaitMinutes() + " mins",
                    te.getChiefComplaint(),
                    te.getCheckInTime() != null ? sdf.format(te.getCheckInTime()) : "-",
                    te.getStatus()
            });
        }

        lblQueueStats.setText("Queue Summary: " + queue.size() + " Waiting | Critical/Urgent (Cat 1-2): "
                + urgentCount + " | Standard/Routine (Cat 3-5): " + standardCount);
    }

    private void doCallNext() {
        List<TriageEntry> queue = triageService.getActiveTriageQueue();
        if (queue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The queue is currently empty. No patients waiting.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        TriageEntry top = queue.get(0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Calling Next Patient:\n\nPatient: " + top.getPatientName() + "\nPriority: "
                        + top.getCategory().getDescription() + "\nDoctor: " + top.getDoctorName()
                        + "\nComplaint: " + top.getChiefComplaint()
                        + "\n\nMark consultation as completed / served?",
                "Call Patient", JOptionPane.YES_NO_OPTION);

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
}
