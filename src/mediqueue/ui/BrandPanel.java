package mediqueue.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Modern clinic brand panel with rich medical teal gradient, glowing cross emblem,
 * polished typography, and key system feature badges.
 */
public class BrandPanel extends JPanel {

    public BrandPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(24, 22, 20, 22));

        // Center content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Medical Logo Emblem
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int size = 52;
                int x = (w - size) / 2;
                int y = (h - size) / 2;

                // Soft outer glowing ring
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillOval(x - 5, y - 5, size + 10, size + 10);

                // Main white circle badge
                g2.setColor(Color.WHITE);
                g2.fillOval(x, y, size, size);

                // Teal medical cross
                g2.setColor(new Color(0x0F766E));
                int crossThick = 7;
                int crossLen = 26;
                int cx = w / 2;
                int cy = h / 2;

                // Horizontal bar
                g2.fillRoundRect(cx - crossLen / 2, cy - crossThick / 2, crossLen, crossThick, 3, 3);
                // Vertical bar
                g2.fillRoundRect(cx - crossThick / 2, cy - crossLen / 2, crossThick, crossLen, 3, 3);

                g2.dispose();
            }
        };
        logoBadge.setPreferredSize(new Dimension(70, 60));
        logoBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        logoBadge.setOpaque(false);
        content.add(logoBadge);

        content.add(Box.createVerticalStrut(6));

        // App Title
        JLabel lblTitle = new JLabel("MediQueue");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblTitle);

        // Subtitle badge
        JLabel lblSub = new JLabel("CLINICAL QUEUE SYSTEM");
        lblSub.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSub.setForeground(new Color(0x99F6E4));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblSub);

        content.add(Box.createVerticalStrut(10));

        // Tagline
        JLabel lblTag = new JLabel("<html><div style='text-align:center;color:#CCFBF1;font-size:11px;width:230px;'>"
                + "Smart clinic queue, triage assessment &amp; conflict-free appointment management."
                + "</div></html>");
        lblTag.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblTag);

        content.add(Box.createVerticalStrut(18));

        // Feature cards (Using clean ASCII badges for reliable cross-platform rendering)
        content.add(buildFeaturePill("MTS", "Manchester Triage System", "Urgency categories 1 to 5"));
        content.add(Box.createVerticalStrut(8));
        content.add(buildFeaturePill("SLOT", "Conflict-Free Booking", "No double-booked time slots"));
        content.add(Box.createVerticalStrut(8));
        content.add(buildFeaturePill("LIVE", "Live Wait Estimation", "Dynamic queue priority & aging"));

        add(content, BorderLayout.CENTER);

        // Bottom badge
        JLabel lblFooter = new JLabel("Secure Authentication & MySQL Protected");
        lblFooter.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(0x99F6E4));
        lblFooter.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblFooter, BorderLayout.SOUTH);
    }

    private JPanel buildFeaturePill(String tag, String title, String desc) {
        JPanel p = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 12, 6, 12));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel lblTag = new JLabel(tag);
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setForeground(new Color(0x5EEAD4));
        p.add(lblTag, BorderLayout.WEST);

        JLabel lblText = new JLabel("<html><b style='color:#FFFFFF;font-size:11px'>" + title + "</b><br>"
                + "<span style='color:#CCFBF1;font-size:9px'>" + desc + "</span></html>");
        p.add(lblText, BorderLayout.CENTER);

        return p;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Elegant diagonal gradient
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(0x0F766E),
                w, h, new Color(0x042F2E)
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Soft decorative circle accents
        g2.setColor(new Color(255, 255, 255, 10));
        g2.fillOval(-30, -30, 160, 160);
        g2.fillOval(w - 90, h - 110, 180, 180);

        g2.dispose();
        super.paintComponent(g);
    }
}
