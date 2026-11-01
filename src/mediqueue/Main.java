package mediqueue;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import mediqueue.ui.LoginFrame;

public class Main {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { new LoginFrame().setVisible(true); }
        });
    }
}
