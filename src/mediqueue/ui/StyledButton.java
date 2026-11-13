package mediqueue.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Custom modern button that reliably paints rich, solid colors with smooth rounded
 * corners across all Java Look & Feels, bypassing the native Windows LookAndFeel issue
 * where buttons become washed-out or transparent white.
 */
public class StyledButton extends JButton {

    private final Color normalBg;
    private final Color hoverBg;
    private final Color pressedBg;
    private int cornerRadius = 6;

    public StyledButton(String text, Color normalBg, Color hoverBg) {
        super(text);
        this.normalBg = normalBg;
        this.hoverBg = hoverBg;
        this.pressedBg = hoverBg.darker();

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public StyledButton(String text, Color normalBg, Color hoverBg, int cornerRadius) {
        this(text, normalBg, hoverBg);
        this.cornerRadius = cornerRadius;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (!isEnabled()) {
            g2.setColor(new Color(0x9CA3AF));
        } else if (getModel().isPressed()) {
            g2.setColor(pressedBg);
        } else if (getModel().isRollover()) {
            g2.setColor(hoverBg);
        } else {
            g2.setColor(normalBg);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        g2.dispose();

        super.paintComponent(g);
    }
}
