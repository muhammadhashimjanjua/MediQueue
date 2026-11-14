package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

/**
 * Modern aesthetic text field with smooth rounded corners, interactive focus glow,
 * placeholder support, and configurable character filtering (alphabet, numeric, alphanumeric).
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private int cornerRadius = 8;
    private boolean hovered = false;

    private static final Color BORDER_NORMAL = new Color(0xD1D5DB);
    private static final Color BORDER_HOVER = new Color(0x9CA3AF);
    private static final Color BORDER_FOCUS = new Color(0x0F766E);
    private static final Color FOCUS_GLOW = new Color(15, 118, 110, 45);
    private static final Color PLACEHOLDER_COLOR = new Color(0x9CA3AF);
    private static final Color BG_COLOR = Color.WHITE;

    public ModernTextField() {
        this("", "");
    }

    public ModernTextField(String placeholder) {
        this("", placeholder);
    }

    public ModernTextField(String text, String placeholder) {
        super(text);
        this.placeholder = placeholder != null ? placeholder : "";

        setOpaque(false);
        setBackground(BG_COLOR);
        setForeground(new Color(0x1F2937));
        setCaretColor(BORDER_FOCUS);
        setFont(new Font("SansSerif", Font.PLAIN, 13));
        setBorder(new EmptyBorder(8, 12, 8, 12));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        repaint();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    /**
     * Enforces that this field accepts ONLY alphabets (letters A-Z, a-z) and spaces.
     * Any non-alphabet keystroke is blocked.
     */
    public void setAlphabetOnly() {
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if (string.matches("^[a-zA-Z\\s]*$")) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                if (text.isEmpty() || text.matches("^[a-zA-Z\\s]*$")) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }

    /**
     * Enforces that this field accepts ONLY numeric digits (0-9).
     * Any non-digit keystroke is blocked.
     */
    public void setNumericOnly(final int maxDigits) {
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if (string.matches("^[0-9]*$") && (fb.getDocument().getLength() + string.length() <= maxDigits)) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                if (text.isEmpty() || (text.matches("^[0-9]*$") && (fb.getDocument().getLength() - length + text.length() <= maxDigits))) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }

    /**
     * Accepts alphanumeric characters (letters and numbers). Both are permitted.
     */
    public void setAlphanumericOnly(final int maxChars) {
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if (string.matches("^[a-zA-Z0-9_]*$") && (fb.getDocument().getLength() + string.length() <= maxChars)) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                if (text.isEmpty() || (text.matches("^[a-zA-Z0-9_]*$") && (fb.getDocument().getLength() - length + text.length() <= maxChars))) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2.setColor(getBackground());
        g2.fillRoundRect(2, 2, w - 4, h - 4, cornerRadius, cornerRadius);

        // Border & Glow
        if (isFocusOwner()) {
            g2.setColor(FOCUS_GLOW);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, cornerRadius, cornerRadius);

            g2.setColor(BORDER_FOCUS);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
        } else if (hovered) {
            g2.setColor(BORDER_HOVER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
        } else {
            g2.setColor(BORDER_NORMAL);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(2, 2, w - 5, h - 5, cornerRadius, cornerRadius);
        }
        g2.dispose();

        super.paintComponent(g);

        // Placeholder text
        if (getText().isEmpty() && !isFocusOwner() && !placeholder.isEmpty()) {
            Graphics2D gp = (Graphics2D) g.create();
            gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gp.setColor(PLACEHOLDER_COLOR);
            gp.setFont(getFont().deriveFont(Font.ITALIC, 12f));
            Insets insets = getInsets();
            FontMetrics fm = gp.getFontMetrics();
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            gp.drawString(placeholder, insets.left + 2, y);
            gp.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Painted directly inside paintComponent for crisp anti-aliased edges
    }
}
