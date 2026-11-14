package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import mediqueue.auth.AuthService;
import mediqueue.auth.AuthService.Role;

/**
 * MediQueue modern aesthetic login window with interactive inputs,
 * real-time character handling, show/hide password toggle, and sleek styling.
 */
public class LoginFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color BUTTON_COLOR = new Color(0x0F766E);
    private static final Color BUTTON_HOVER = new Color(0x115E59);
    private static final Color ERROR_BG = new Color(0xFEF2F2);
    private static final Color ERROR_BORDER = new Color(0xFCA5A5);
    private static final Color ERROR_TEXT = new Color(0x991B1B);

    private final AuthService auth = new AuthService();

    private final ModernTextField txtUser = new ModernTextField("Enter your username");
    private final ModernPasswordField txtPass = new ModernPasswordField("Enter your password");
    private final JComboBox<Role> cmbRole = new JComboBox<Role>(Role.values());
    private final JCheckBox chkShow = new JCheckBox("Show password");
    private final StyledButton btnToggleShow = new StyledButton("Show", new Color(0xF3F4F6), new Color(0xE5E7EB), 8);

    private final JPanel pnlError = new JPanel(new BorderLayout(6, 0));
    private final JLabel lblError = new JLabel(" ");

    private final StyledButton btnLogin = new StyledButton("Sign In to MediQueue", BUTTON_COLOR, BUTTON_HOVER, 8);
    private final JButton btnGoSignUp = new JButton("Don't have an account? Sign Up");

    public LoginFrame() {
        this("");
    }

    public LoginFrame(String initialUsername) {
        super("MediQueue - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(btnLogin); // Enter key = Log In
        pack();
        setSize(800, 530);
        setResizable(false);
        setLocationRelativeTo(null);

        if (initialUsername != null && !initialUsername.trim().isEmpty()) {
            txtUser.setText(initialUsername.trim());
            txtPass.requestFocusInWindow();
        }
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(new BrandPanel());
        root.add(buildFormPanel());
        return root;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(24, 36, 24, 36));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);

        // Header Title & Subtitle
        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(0x111827));

        JLabel sub = new JLabel("Sign in with your role and clinic credentials");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(0x6B7280));

        // Form Fields Styling
        Dimension fieldDim = new Dimension(100, 36);
        txtUser.setPreferredSize(fieldDim);
        txtPass.setPreferredSize(fieldDim);
        txtUser.setAlphanumericOnly(30); // Accepts both letters and numbers

        // Role Dropdown styling
        cmbRole.setPreferredSize(fieldDim);
        cmbRole.setBackground(Color.WHITE);
        cmbRole.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cmbRole.setFocusable(false);
        cmbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (isSelected) {
                    lbl.setBackground(new Color(0xCCFBF1));
                    lbl.setForeground(new Color(0x0F766E));
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(0x1F2937));
                }
                return lbl;
            }
        });

        // Password Row with modern Show/Hide toggle button
        btnToggleShow.setPreferredSize(new Dimension(84, 36));
        btnToggleShow.setForeground(new Color(0x374151));
        btnToggleShow.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnToggleShow.setToolTipText("Toggle password visibility");

        JPanel pnlPasswordRow = new JPanel(new BorderLayout(6, 0));
        pnlPasswordRow.setOpaque(false);
        pnlPasswordRow.add(txtPass, BorderLayout.CENTER);
        pnlPasswordRow.add(btnToggleShow, BorderLayout.EAST);

        // Checkbox Show
        chkShow.setBackground(Color.WHITE);
        chkShow.setFocusPainted(false);
        chkShow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkShow.setForeground(new Color(0x4B5563));

        // Error Banner Panel
        pnlError.setBackground(ERROR_BG);
        pnlError.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ERROR_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        lblError.setForeground(ERROR_TEXT);
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel lblErrorIcon = new JLabel("⚠ ");
        lblErrorIcon.setForeground(ERROR_TEXT);
        pnlError.add(lblErrorIcon, BorderLayout.WEST);
        pnlError.add(lblError, BorderLayout.CENTER);
        pnlError.setVisible(false);

        // Action Buttons
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(100, 40));

        btnGoSignUp.setForeground(PRIMARY);
        btnGoSignUp.setBackground(Color.WHITE);
        btnGoSignUp.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGoSignUp.setBorderPainted(false);
        btnGoSignUp.setContentAreaFilled(false);
        btnGoSignUp.setFocusPainted(false);
        btnGoSignUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGoSignUp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnGoSignUp.setText("<html><u>Don't have an account? Sign Up</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnGoSignUp.setText("Don't have an account? Sign Up");
            }
        });

        // Demo hint pill
        JPanel pnlDemo = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        pnlDemo.setOpaque(false);
        JLabel hint = new JLabel("<html><span style='color:#6B7280;font-size:11px'>"
                + "Demo: <b style='color:#0F766E'>doctor</b> / <b style='color:#0F766E'>doctor123</b> (Role: Doctor)</span></html>");
        pnlDemo.add(hint);

        // Labels with field rule hints
        JLabel lblRoleTitle = new JLabel("Role");
        lblRoleTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblRoleTitle.setForeground(new Color(0x374151));

        JLabel lblUserTitle = new JLabel("<html><b>Username</b> <span style='color:#9CA3AF;font-size:10px;'>(letters &amp; numbers accepted)</span></html>");
        lblUserTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblUserTitle.setForeground(new Color(0x374151));

        JLabel lblPassTitle = new JLabel("<html><b>Password</b> <span style='color:#9CA3AF;font-size:10px;'>(letters &amp; numbers accepted)</span></html>");
        lblPassTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPassTitle.setForeground(new Color(0x374151));

        // Assemble Layout
        int row = 0;
        c.gridy = row++; p.add(title, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0); p.add(sub, c);

        c.insets = new Insets(2, 0, 2, 0);
        c.gridy = row++; p.add(lblRoleTitle, c);
        c.gridy = row++; p.add(cmbRole, c);

        c.insets = new Insets(4, 0, 2, 0);
        c.gridy = row++; p.add(lblUserTitle, c);
        c.gridy = row++; p.add(txtUser, c);

        c.insets = new Insets(4, 0, 2, 0);
        c.gridy = row++; p.add(lblPassTitle, c);
        c.gridy = row++; p.add(pnlPasswordRow, c);

        c.insets = new Insets(2, 0, 4, 0);
        c.gridy = row++; p.add(chkShow, c);

        c.insets = new Insets(4, 0, 6, 0);
        c.gridy = row++; p.add(pnlError, c);

        c.insets = new Insets(6, 0, 4, 0);
        c.gridy = row++; p.add(btnLogin, c);

        c.insets = new Insets(4, 0, 2, 0);
        c.gridy = row++; p.add(btnGoSignUp, c);

        c.insets = new Insets(2, 0, 0, 0);
        c.gridy = row++; p.add(pnlDemo, c);

        // Interactive Show / Hide Password Actions
        final char defaultEcho = txtPass.getEchoChar();
        ActionListener toggleAction = new ActionListener() {
            private boolean visible = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == chkShow) {
                    visible = chkShow.isSelected();
                } else {
                    visible = !visible;
                    chkShow.setSelected(visible);
                }
                txtPass.setEchoChar(visible ? (char) 0 : defaultEcho);
                btnToggleShow.setText(visible ? "Hide" : "Show");
            }
        };

        chkShow.addActionListener(toggleAction);
        btnToggleShow.addActionListener(toggleAction);

        // Clear error banner when user begins typing
        KeyAdapter clearError = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (pnlError.isVisible()) {
                    pnlError.setVisible(false);
                }
            }
        };
        txtUser.addKeyListener(clearError);
        txtPass.addKeyListener(clearError);

        // Actions
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        btnGoSignUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SignUpFrame().setVisible(true);
                dispose();
            }
        });

        return p;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        pnlError.setVisible(true);
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        char[] pass = txtPass.getPassword();

        if (user.isEmpty()) {
            showError("Please enter your username.");
            txtUser.requestFocusInWindow();
            return;
        }

        if (pass.length == 0) {
            showError("Please enter your password.");
            txtPass.requestFocusInWindow();
            return;
        }

        Role role = (Role) cmbRole.getSelectedItem();
        String fullName = auth.login(user, pass, role);
        Arrays.fill(pass, '\0'); // clear password from memory

        if (fullName == null) {
            String err = auth.getLastError();
            showError(err != null ? err : "Invalid username, password or role.");
            txtPass.setText("");
            txtPass.requestFocusInWindow();
            return;
        }

        pnlError.setVisible(false);
        new DashboardFrame(fullName, role).setVisible(true);
        dispose();
    }
}
