package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import mediqueue.auth.AuthService;
import mediqueue.auth.AuthService.Role;

/**
 * MediQueue modern aesthetic registration window with strict field typing:
 * - Full Name: Alphabet characters only.
 * - Phone Number: Numeric digits only.
 * - Username: Accepts both letters and numbers.
 * - Password: Authenticated with both letters and numbers required.
 * - Show / Hide password options on both fields.
 */
public class SignUpFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color BUTTON_COLOR = new Color(0x0F766E);
    private static final Color BUTTON_HOVER = new Color(0x115E59);
    private static final Color ERROR_BG = new Color(0xFEF2F2);
    private static final Color ERROR_BORDER = new Color(0xFCA5A5);
    private static final Color ERROR_TEXT = new Color(0x991B1B);

    private final AuthService auth = new AuthService();

    private final ModernTextField txtFullName = new ModernTextField("e.g. Muhammad Hashim");
    private final ModernTextField txtPhone = new ModernTextField("e.g. 03001234567");
    private final ModernTextField txtUser = new ModernTextField("e.g. hashim123");
    private final JComboBox<Role> cmbRole = new JComboBox<Role>(Role.values());

    private final ModernPasswordField txtPass = new ModernPasswordField("Enter password (letters & numbers)");
    private final ModernPasswordField txtConfirmPass = new ModernPasswordField("Confirm your password");

    private final StyledButton btnTogglePass = new StyledButton("Show", new Color(0xF3F4F6), new Color(0xE5E7EB), 8);
    private final StyledButton btnToggleConfirm = new StyledButton("Show", new Color(0xF3F4F6), new Color(0xE5E7EB), 8);

    private final JCheckBox chkShow = new JCheckBox("Show passwords");

    private final JPanel pnlError = new JPanel(new BorderLayout(6, 0));
    private final JLabel lblError = new JLabel(" ");

    private final StyledButton btnSignUp = new StyledButton("Create MediQueue Account", BUTTON_COLOR, BUTTON_HOVER, 8);
    private final JButton btnGoLogin = new JButton("Already have an account? Log In");

    public SignUpFrame() {
        super("MediQueue - Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(btnSignUp);
        pack();
        setSize(840, 660);
        setResizable(false);
        setLocationRelativeTo(null);
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
        p.setBorder(new EmptyBorder(20, 34, 20, 34));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);

        // Header Title & Subtitle
        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(0x111827));

        JLabel sub = new JLabel("Register a new clinic staff or patient account");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(0x6B7280));

        // Form Fields Sizing & Strict Filters
        Dimension fieldDim = new Dimension(100, 32);
        txtFullName.setPreferredSize(fieldDim);
        txtPhone.setPreferredSize(fieldDim);
        txtUser.setPreferredSize(fieldDim);
        txtPass.setPreferredSize(fieldDim);
        txtConfirmPass.setPreferredSize(fieldDim);

        // 1. Alphabet Column: Only accepts alphabets (letters and spaces)
        txtFullName.setAlphabetOnly();
        txtFullName.setToolTipText("Alphabet only: Enter letters A-Z and spaces");

        // 2. Numeric Block: Only accepts numbers (digits 0-9)
        txtPhone.setNumericOnly(15);
        txtPhone.setToolTipText("Numeric block: Enter numbers 0-9 only");

        // 3. Username: Accepts both letters and numbers
        txtUser.setAlphanumericOnly(30);
        txtUser.setToolTipText("Alphanumeric: Both letters and numbers accepted");

        // Role Dropdown styling
        cmbRole.setPreferredSize(fieldDim);
        cmbRole.setBackground(Color.WHITE);
        cmbRole.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbRole.setFocusable(false);
        cmbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(5, 8, 5, 8));
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

        // Password Row with Toggle Button
        btnTogglePass.setPreferredSize(new Dimension(80, 32));
        btnTogglePass.setForeground(new Color(0x374151));
        btnTogglePass.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnTogglePass.setToolTipText("Show/hide password");

        JPanel pnlPassRow = new JPanel(new BorderLayout(6, 0));
        pnlPassRow.setOpaque(false);
        pnlPassRow.add(txtPass, BorderLayout.CENTER);
        pnlPassRow.add(btnTogglePass, BorderLayout.EAST);

        // Confirm Password Row with Toggle Button
        btnToggleConfirm.setPreferredSize(new Dimension(80, 32));
        btnToggleConfirm.setForeground(new Color(0x374151));
        btnToggleConfirm.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnToggleConfirm.setToolTipText("Show/hide confirm password");

        JPanel pnlConfirmRow = new JPanel(new BorderLayout(6, 0));
        pnlConfirmRow.setOpaque(false);
        pnlConfirmRow.add(txtConfirmPass, BorderLayout.CENTER);
        pnlConfirmRow.add(btnToggleConfirm, BorderLayout.EAST);

        // Show Passwords Checkbox
        chkShow.setBackground(Color.WHITE);
        chkShow.setFocusPainted(false);
        chkShow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkShow.setForeground(new Color(0x4B5563));

        // Error Banner Panel
        pnlError.setBackground(ERROR_BG);
        pnlError.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ERROR_BORDER, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        lblError.setForeground(ERROR_TEXT);
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel lblErrorIcon = new JLabel("⚠ ");
        lblErrorIcon.setForeground(ERROR_TEXT);
        pnlError.add(lblErrorIcon, BorderLayout.WEST);
        pnlError.add(lblError, BorderLayout.CENTER);
        pnlError.setVisible(false);

        // Action Buttons
        btnSignUp.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSignUp.setPreferredSize(new Dimension(100, 38));

        btnGoLogin.setForeground(PRIMARY);
        btnGoLogin.setBackground(Color.WHITE);
        btnGoLogin.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGoLogin.setBorderPainted(false);
        btnGoLogin.setContentAreaFilled(false);
        btnGoLogin.setFocusPainted(false);
        btnGoLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGoLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnGoLogin.setText("<html><u>Already have an account? Log In</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnGoLogin.setText("Already have an account? Log In");
            }
        });

        // Field Label Badges with Validation Indicators
        JLabel lblNameTitle = new JLabel("<html><b>Full Name</b> <span style='color:#0F766E;font-size:10px;'>[Alphabet Only: letters &amp; spaces]</span></html>");
        lblNameTitle.setForeground(new Color(0x374151));

        JLabel lblPhoneTitle = new JLabel("<html><b>Phone Number</b> <span style='color:#0F766E;font-size:10px;'>[Numeric Block: numbers only]</span></html>");
        lblPhoneTitle.setForeground(new Color(0x374151));

        JLabel lblUserTitle = new JLabel("<html><b>Username</b> <span style='color:#0F766E;font-size:10px;'>[Both: letters &amp; numbers]</span></html>");
        lblUserTitle.setForeground(new Color(0x374151));

        JLabel lblRoleTitle = new JLabel("<html><b>Role</b></html>");
        lblRoleTitle.setForeground(new Color(0x374151));

        JLabel lblPassTitle = new JLabel("<html><b>Password</b> <span style='color:#0F766E;font-size:10px;'>[Both: min 6 chars, letters &amp; numbers]</span></html>");
        lblPassTitle.setForeground(new Color(0x374151));

        JLabel lblConfirmTitle = new JLabel("<html><b>Confirm Password</b></html>");
        lblConfirmTitle.setForeground(new Color(0x374151));

        // Assemble Layout
        int row = 0;
        c.gridy = row++; p.add(title, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0); p.add(sub, c);

        c.insets = new Insets(2, 0, 2, 0);
        c.gridy = row++; p.add(lblNameTitle, c);
        c.gridy = row++; p.add(txtFullName, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; p.add(lblPhoneTitle, c);
        c.gridy = row++; p.add(txtPhone, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; p.add(lblUserTitle, c);
        c.gridy = row++; p.add(txtUser, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; p.add(lblRoleTitle, c);
        c.gridy = row++; p.add(cmbRole, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; p.add(lblPassTitle, c);
        c.gridy = row++; p.add(pnlPassRow, c);

        c.insets = new Insets(3, 0, 2, 0);
        c.gridy = row++; p.add(lblConfirmTitle, c);
        c.gridy = row++; p.add(pnlConfirmRow, c);

        c.insets = new Insets(2, 0, 2, 0);
        c.gridy = row++; p.add(chkShow, c);

        c.insets = new Insets(3, 0, 4, 0);
        c.gridy = row++; p.add(pnlError, c);

        c.insets = new Insets(4, 0, 3, 0);
        c.gridy = row++; p.add(btnSignUp, c);

        c.insets = new Insets(2, 0, 0, 0);
        c.gridy = row++; p.add(btnGoLogin, c);

        // Interactive Show / Hide Password Actions
        final char defaultEcho = txtPass.getEchoChar();

        // 1. Password field toggle button
        btnTogglePass.addActionListener(new ActionListener() {
            private boolean visible = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                visible = !visible;
                txtPass.setEchoChar(visible ? (char) 0 : defaultEcho);
                btnTogglePass.setText(visible ? "Hide" : "Show");
            }
        });

        // 2. Confirm Password field toggle button
        btnToggleConfirm.addActionListener(new ActionListener() {
            private boolean visible = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                visible = !visible;
                txtConfirmPass.setEchoChar(visible ? (char) 0 : defaultEcho);
                btnToggleConfirm.setText(visible ? "Hide" : "Show");
            }
        });

        // 3. Checkbox toggles both simultaneously
        chkShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean visible = chkShow.isSelected();
                char echo = visible ? (char) 0 : defaultEcho;
                txtPass.setEchoChar(echo);
                txtConfirmPass.setEchoChar(echo);
                btnTogglePass.setText(visible ? "Hide" : "Show");
                btnToggleConfirm.setText(visible ? "Hide" : "Show");
            }
        });

        // Clear error banner when user begins typing
        KeyAdapter clearError = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (pnlError.isVisible()) {
                    pnlError.setVisible(false);
                }
            }
        };
        txtFullName.addKeyListener(clearError);
        txtPhone.addKeyListener(clearError);
        txtUser.addKeyListener(clearError);
        txtPass.addKeyListener(clearError);
        txtConfirmPass.addKeyListener(clearError);

        // Actions
        btnSignUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSignUp();
            }
        });

        btnGoLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        return p;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        pnlError.setVisible(true);
    }

    private void doSignUp() {
        String fullName = txtFullName.getText().trim();
        String phone = txtPhone.getText().trim();
        String user = txtUser.getText().trim();
        char[] pass = txtPass.getPassword();
        char[] confirmPass = txtConfirmPass.getPassword();
        Role role = (Role) cmbRole.getSelectedItem();

        // 1. Validate Alphabet Column (Full Name)
        if (fullName.isEmpty()) {
            showError("Please enter your full name.");
            txtFullName.requestFocusInWindow();
            return;
        }
        if (!AuthService.isAlphabetOnly(fullName)) {
            showError("Full Name must contain only alphabets (letters and spaces).");
            txtFullName.requestFocusInWindow();
            return;
        }

        // 2. Validate Numeric Block (Phone Number)
        if (phone.isEmpty()) {
            showError("Please enter your phone number.");
            txtPhone.requestFocusInWindow();
            return;
        }
        if (!AuthService.isNumericOnly(phone)) {
            showError("Phone Number must contain only numbers (digits 0-9).");
            txtPhone.requestFocusInWindow();
            return;
        }
        if (phone.length() < 7 || phone.length() > 15) {
            showError("Phone Number must be between 7 and 15 digits.");
            txtPhone.requestFocusInWindow();
            return;
        }

        // 3. Validate Username (Both letters and numbers accepted)
        if (user.isEmpty()) {
            showError("Please enter a username.");
            txtUser.requestFocusInWindow();
            return;
        }
        if (user.length() < 3) {
            showError("Username must be at least 3 characters.");
            txtUser.requestFocusInWindow();
            return;
        }
        if (!AuthService.isAlphanumeric(user)) {
            showError("Username can accept both letters and numbers.");
            txtUser.requestFocusInWindow();
            return;
        }

        // 4. Validate Password (Both letters and numbers required)
        if (pass.length == 0) {
            showError("Please enter a password.");
            txtPass.requestFocusInWindow();
            return;
        }
        String passStr = new String(pass);
        if (passStr.length() < 6) {
            showError("Password must be at least 6 characters.");
            txtPass.requestFocusInWindow();
            return;
        }
        if (!AuthService.containsBothLettersAndNumbers(passStr)) {
            showError("Password must contain both letters and numbers.");
            txtPass.requestFocusInWindow();
            return;
        }

        // 5. Confirm Password matching
        if (!Arrays.equals(pass, confirmPass)) {
            showError("Passwords do not match!");
            txtConfirmPass.setText("");
            txtConfirmPass.requestFocusInWindow();
            return;
        }

        // 6. Register User via AuthService
        boolean ok = auth.register(fullName, phone, user, pass, role);
        Arrays.fill(pass, '\0');
        Arrays.fill(confirmPass, '\0');

        if (!ok) {
            String err = auth.getLastError();
            showError(err != null ? err : "Registration failed.");
            return;
        }

        pnlError.setVisible(false);
        JOptionPane.showMessageDialog(this,
                "<html><b>Account created successfully!</b><br>"
                + "User: <b>" + user + "</b> (" + fullName + ")<br>"
                + "Role: <b>" + role + "</b><br><br>"
                + "You can now log in with your credentials.</html>",
                "Registration Successful",
                JOptionPane.INFORMATION_MESSAGE);

        new LoginFrame(user).setVisible(true);
        dispose();
    }
}
