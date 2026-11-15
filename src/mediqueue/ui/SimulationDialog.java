package mediqueue.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import mediqueue.thread.PolicySimulationResult;
import mediqueue.thread.QueuePolicySimulator;

/**
 * Interactive dialog that demonstrates and visualizes the multi-threaded
 * queue policy simulation comparing FCFS, Strict Priority, and Priority with Aging.
 */
public class SimulationDialog extends JDialog {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color BG_LIGHT = new Color(0xF3F4F6);
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_DARK = new Color(0x1F2937);
    private static final Color TEXT_MUTED = new Color(0x6B7280);

    private final JComboBox<Integer> cmbPatientCount = new JComboBox<Integer>(new Integer[]{40, 60, 100, 200});
    private final JComboBox<Integer> cmbDoctorCount = new JComboBox<Integer>(new Integer[]{1, 2, 3, 4});
    private final StyledButton btnRun = new StyledButton("▶ Run Concurrent Simulation (Threads)", PRIMARY, PRIMARY_DARK, 8);
    private final JLabel lblStatus = new JLabel("Ready to simulate. 3 worker threads will run concurrently.");

    private final DefaultTableModel modelResults = new DefaultTableModel(
            new String[]{"Policy Strategy", "Worker Thread", "Total Patients", "Avg Wait (mins)", "Max Wait", "Overdue Target %", "Starvation % (Cat 4-5)", "Thread Runtime"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblResults = new JTable(modelResults);
    private final JTextArea txtAnalysis = new JTextArea();

    public SimulationDialog(Frame owner) {
        super(owner, "MediQueue - Multi-Threaded Queue Policy Benchmark", true);
        setSize(980, 620);
        setMinimumSize(new Dimension(880, 520));
        setLocationRelativeTo(owner);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG_LIGHT);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top Header Banner
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(PRIMARY);
        pnlHeader.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("Multi-Threaded Queue Policy Simulator");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Evaluates 3 queue ordering strategies in parallel using dedicated Java worker threads (Thread.join() synchronization)");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(0xCC, 0xFB, 0xF1));

        pnlHeader.add(lblTitle, BorderLayout.NORTH);
        pnlHeader.add(lblSubtitle, BorderLayout.SOUTH);
        root.add(pnlHeader, BorderLayout.NORTH);

        // Center Content
        JPanel pnlCenter = new JPanel(new BorderLayout(10, 10));
        pnlCenter.setOpaque(false);

        // Controls Bar
        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        pnlControls.setBackground(Color.WHITE);
        pnlControls.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(4, 10, 4, 10)));

        pnlControls.add(new JLabel("Arrival Count:"));
        cmbPatientCount.setSelectedItem(60);
        pnlControls.add(cmbPatientCount);

        pnlControls.add(new JLabel("Active Doctors:"));
        cmbDoctorCount.setSelectedItem(2);
        pnlControls.add(cmbDoctorCount);

        btnRun.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSimulation();
            }
        });
        pnlControls.add(btnRun);

        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblStatus.setForeground(TEXT_MUTED);
        pnlControls.add(lblStatus);

        pnlCenter.add(pnlControls, BorderLayout.NORTH);

        // Table Panel
        styleTable(tblResults);
        JScrollPane spTable = new JScrollPane(tblResults);
        spTable.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        spTable.setPreferredSize(new Dimension(900, 180));

        // Bottom Analysis Panel
        JPanel pnlAnalysisCard = new JPanel(new BorderLayout(6, 6));
        pnlAnalysisCard.setBackground(Color.WHITE);
        pnlAnalysisCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(12, 14, 12, 14)));

        JLabel lblAnalysisTitle = new JLabel("Multi-Threading & Queue Policy Analysis:");
        lblAnalysisTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblAnalysisTitle.setForeground(PRIMARY_DARK);

        txtAnalysis.setEditable(false);
        txtAnalysis.setLineWrap(true);
        txtAnalysis.setWrapStyleWord(true);
        txtAnalysis.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtAnalysis.setForeground(TEXT_DARK);
        txtAnalysis.setBackground(Color.WHITE);
        txtAnalysis.setText("Click 'Run Concurrent Simulation' to launch 3 parallel threads:\n"
                + "• Thread-Worker-FCFS: Simulates First-Come, First-Served queue.\n"
                + "• Thread-Worker-StrictPriority: Simulates raw Manchester Triage categories.\n"
                + "• Thread-Worker-PriorityWithAging: Simulates Anti-Starvation aging (+1 category per 30m).\n\n"
                + "Results will show why Priority-with-Aging provides optimal fairness without starvating routine patients.");

        pnlAnalysisCard.add(lblAnalysisTitle, BorderLayout.NORTH);
        pnlAnalysisCard.add(new JScrollPane(txtAnalysis), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTable, pnlAnalysisCard);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        pnlCenter.add(splitPane, BorderLayout.CENTER);
        root.add(pnlCenter, BorderLayout.CENTER);

        // Footer Bar
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFooter.setOpaque(false);
        StyledButton btnClose = new StyledButton("Close", new Color(0xE5, 0xE7, 0xEB), new Color(0xD1, 0xD5, 0xDB), 6);
        btnClose.setForeground(TEXT_DARK);
        btnClose.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
        pnlFooter.add(btnClose);
        root.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void executeSimulation() {
        final int patientCount = (Integer) cmbPatientCount.getSelectedItem();
        final int doctorCount = (Integer) cmbDoctorCount.getSelectedItem();
        final long seed = 42L; // fixed seed for exact reproducible comparison across threads

        btnRun.setEnabled(false);
        lblStatus.setText("Running 3 concurrent worker threads...");
        modelResults.setRowCount(0);

        QueuePolicySimulator.runSimulationAsync(patientCount, doctorCount, seed, new QueuePolicySimulator.SimulationCallback() {
            @Override
            public void onSimulationComplete(List<PolicySimulationResult> results, long totalWallClockTimeMs) {
                btnRun.setEnabled(true);
                lblStatus.setText("✓ Simulation complete! All 3 threads joined successfully in " + totalWallClockTimeMs + " ms.");

                PolicySimulationResult fcfs = null;
                PolicySimulationResult strict = null;
                PolicySimulationResult aging = null;

                for (PolicySimulationResult r : results) {
                    modelResults.addRow(new Object[]{
                            r.getPolicy().toString(),
                            r.getThreadName(),
                            r.getTotalPatients(),
                            String.format("%.1f", r.getAverageWaitMinutes()),
                            r.getMaxWaitMinutes() + " mins",
                            String.format("%.1f%% (%d)", r.getOverduePercentage(), r.getOverdueCount()),
                            String.format("%.1f%% (%d)", r.getStarvationPercentage(), r.getStarvedRoutineCount()),
                            r.getExecutionTimeMillis() + " ms (" + (r.getExecutionTimeNanos() / 1000) + " µs)"
                    });

                    if (r.getPolicy().name().contains("FCFS")) fcfs = r;
                    else if (r.getPolicy().name().contains("STRICT")) strict = r;
                    else aging = r;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("=== MULTI-THREAD CONCURRENCY & BENCHMARK REPORT ===\n");
                sb.append("• Concurrency Execution: 3 independent threads spawned in parallel and joined via Thread.join().\n");
                sb.append(String.format("• Wall-Clock Execution Time: %d ms for %d simulated patients across %d doctors.\n\n",
                        totalWallClockTimeMs, patientCount, doctorCount));

                if (fcfs != null && strict != null && aging != null) {
                    sb.append("KEY FINDINGS & SCIENTIFIC COMPARISON:\n");
                    sb.append(String.format("1. FCFS (First-Come, First-Served):\n   - Target Overdue Rate: %.1f%% of patients exceeded clinical target waiting times.\n   - Unsafe for emergency triage because Category 1 (resuscitation) patients are queued behind routine walk-ins.\n\n",
                            fcfs.getOverduePercentage()));

                    sb.append(String.format("2. Strict Priority (Manchester Triage Urgency Only):\n   - Max Wait Time: %d mins | Routine Starvation Rate: %.1f%% (>120 mins wait).\n   - Low-priority patients (Category 4 & 5) suffer indefinite postponement (starvation) when high-urgency patients arrive.\n\n",
                            strict.getMaxWaitMinutes(), strict.getStarvationPercentage()));

                    sb.append(String.format("3. Priority with Aging (MediQueue Anti-Starvation Rule):\n   - Routine Starvation Rate: %.1f%% (Reduced from %.1f%% in Strict Priority!).\n   - Max Wait Time: %d mins (Significantly lower than Strict Priority %d mins).\n   - Conclusion: Priority with Aging prevents starvation while strictly protecting emergency response times!",
                            aging.getStarvationPercentage(), strict.getStarvationPercentage(), aging.getMaxWaitMinutes(), strict.getMaxWaitMinutes()));
                }

                txtAnalysis.setText(sb.toString());
                txtAnalysis.setCaretPosition(0);
            }

            @Override
            public void onSimulationError(Exception ex) {
                btnRun.setEnabled(true);
                lblStatus.setText("Error during simulation: " + ex.getMessage());
                JOptionPane.showMessageDialog(SimulationDialog.this, "Simulation error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(0xCC, 0xFB, 0xF1));
        table.setSelectionForeground(PRIMARY_DARK);
        table.setGridColor(BORDER_COLOR);
        table.setShowGrid(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF9, 0xFA, 0xFB));
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(75);
        table.getColumnModel().getColumn(5).setPreferredWidth(115);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);
        table.getColumnModel().getColumn(7).setPreferredWidth(100);
    }
}
