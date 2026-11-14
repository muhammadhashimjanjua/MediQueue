package mediqueue.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import mediqueue.auth.AuthService.Role;

public class TestAgingRender {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    DashboardFrame dash = new DashboardFrame("Dr. Hamza Malik", Role.DOCTOR);
                    dash.addNotify();
                    dash.setSize(1180, 760);
                    dash.validate();
                    dash.doLayout();

                    JTabbedPane tabs = (JTabbedPane) ((JPanel) dash.getContentPane()).getComponent(1);
                    tabs.setSelectedIndex(1);
                    dash.validate();
                    dash.doLayout();

                    BufferedImage imgTriage = new BufferedImage(1180, 760, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gTriage = imgTriage.createGraphics();
                    dash.getContentPane().printAll(gTriage);
                    gTriage.dispose();
                    ImageIO.write(imgTriage, "png", new File("C:/Users/janju/Desktop/MediQueue/dashboard_triage_aging_preview.png"));
                    dash.dispose();
                    System.out.println("AGING_RENDER_DONE");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}