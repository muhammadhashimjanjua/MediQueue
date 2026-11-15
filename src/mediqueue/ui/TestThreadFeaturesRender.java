package mediqueue.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import mediqueue.auth.AuthService.Role;

public class TestThreadFeaturesRender {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Render Dashboard with Live Monitor Thread button
                    DashboardFrame dash = new DashboardFrame("Dr. Hamza Malik", Role.DOCTOR);
                    dash.addNotify();
                    dash.setSize(1240, 780);
                    dash.validate();
                    dash.doLayout();

                    JTabbedPane tabs = (JTabbedPane) ((JPanel) dash.getContentPane()).getComponent(1);
                    tabs.setSelectedIndex(1);
                    dash.validate();
                    dash.doLayout();

                    BufferedImage imgDash = new BufferedImage(1240, 780, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gDash = imgDash.createGraphics();
                    dash.getContentPane().printAll(gDash);
                    gDash.dispose();
                    ImageIO.write(imgDash, "png", new File("C:/Users/janju/Desktop/MediQueue/dashboard_thread_monitor_preview.png"));

                    // 2. Render SimulationDialog with Multi-Threaded Results
                    SimulationDialog dlg = new SimulationDialog(dash);
                    dlg.addNotify();
                    dlg.setSize(980, 620);
                    dlg.validate();
                    dlg.doLayout();

                    // Trigger the simulation
                    for (Component comp : ((JPanel) ((JPanel) ((JPanel) dlg.getContentPane()).getComponent(1)).getComponent(0)).getComponents()) {
                        if (comp instanceof StyledButton) {
                            ((StyledButton) comp).doClick();
                            break;
                        }
                    }

                    // Wait a moment for background threads to join
                    Timer timer = new Timer(500, new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            try {
                                dlg.validate();
                                dlg.doLayout();
                                BufferedImage imgDlg = new BufferedImage(980, 620, BufferedImage.TYPE_INT_RGB);
                                Graphics2D gDlg = imgDlg.createGraphics();
                                dlg.getContentPane().printAll(gDlg);
                                gDlg.dispose();
                                ImageIO.write(imgDlg, "png", new File("C:/Users/janju/Desktop/MediQueue/simulation_threads_preview.png"));
                                dlg.dispose();
                                dash.dispose();
                                System.out.println("THREAD_RENDER_COMPLETE");
                                System.exit(0);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                System.exit(1);
                            }
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();

                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }
}
