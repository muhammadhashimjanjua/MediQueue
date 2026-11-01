package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import mediqueue.auth.AuthService;
import mediqueue.auth.AuthService.Role;

/**
 * MediQueue registration window (Swing).
 */
public class SignUpFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color ERROR = new Color(0xB91C1C);

    private final AuthService auth = new AuthService();

    private final JTextField txtFullName = new JTextField();
    private final JTextField txtUser = new JTextField();
    private final JComboBox<Role> cmbRole = new JComboBox<Role>(Role.values());
    private final JPasswordField txtPass = new JPasswordField();
    private final JPasswordField txtConfirmPass = new JPasswordField();
    private final JCheckBox chkShow = new JCheckBox("Show passwords");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSignUp = new JButton("Create Account");
    private final JButton btnGoLogin = new JButton("Already have an account? Log In");

    public SignUpFrame() {
        super("MediQueue - Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(btnSignUp);
        pack();
        setSize(780, 560);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
        return root;
    }

    private JPanel buildBrandPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(PRIMARY);
        JLabel logo = new JLabel("+");
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        JLabel name = new JLabel("MediQueue");
        name.setFont(new Font("SansSerif", Font.BOLD, 30));
        name.setForeground(Color.WHITE);
        JLabel tag = new JLabel("<html><div style='text-align:center'>Smart clinic queue &amp;<br>appointment management</div></html>");
        tag.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tag.setForeground(new Color(0xCCFBF1));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(4, 0, 4, 0);
        p.add(logo, c);
        p.add(name, c);
        p.add(tag, c);
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 32, 16, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel sub = new JLabel("Sign up to register a new user");
        sub.setForeground(Color.GRAY);

        Dimension field = new Dimension(100, 28);
        txtFullName.setPreferredSize(field);
        txtUser.setPreferredSize(field);
        cmbRole.setPreferredSize(field);
        cmbRole.setBackground(Color.WHITE);
        txtPass.setPreferredSize(field);
        txtConfirmPass.setPreferredSize(field);

        chkShow.setBackground(Color.WHITE);
        chkShow.setFocusPainted(false);

        lblError.setForeground(ERROR);
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 11));

        btnSignUp.setBackground(PRIMARY);
        btnSignUp.setForeground(Color.WHITE);
        btnSignUp.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSignUp.setFocusPainted(false);
        btnSignUp.setBorder(new EmptyBorder(8, 0, 8, 0));
        btnSignUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignUp.setOpaque(true);
        btnSignUp.setBorderPainted(false);
        btnSignUp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnSignUp.setBackground(PRIMARY_DARK); }
            @Override
            public void mouseExited(MouseEvent e) { btnSignUp.setBackground(PRIMARY); }
        });

        btnGoLogin.setForeground(PRIMARY);
        btnGoLogin.setBackground(Color.WHITE);
        btnGoLogin.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnGoLogin.setBorderPainted(false);
        btnGoLogin.setContentAreaFilled(false);
        btnGoLogin.setFocusPainted(false);
        btnGoLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        int row = 0;
        c.gridy = row++; p.add(title, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0); p.add(sub, c);
        c.insets = new Insets(2, 0, 2, 0);

        c.gridy = row++; p.add(new JLabel("Full Name"), c);
        c.gridy = row++; p.add(txtFullName, c);

        c.gridy = row++; p.add(new JLabel("Username"), c);
        c.gridy = row++; p.add(txtUser, c);

        c.gridy = row++; p.add(new JLabel("Role"), c);
        c.gridy = row++; p.add(cmbRole, c);

        c.gridy = row++; p.add(new JLabel("Password"), c);
        c.gridy = row++; p.add(txtPass, c);

        c.gridy = row++; p.add(new JLabel("Confirm Password"), c);
        c.gridy = row++; p.add(txtConfirmPass, c);

        c.gridy = row++; p.add(chkShow, c);
        c.gridy = row++; p.add(lblError, c);
        c.gridy = row++; p.add(btnSignUp, c);
        c.gridy = row++; c.insets = new Insets(4, 0, 0, 0); p.add(btnGoLogin, c);

        final char defaultEcho = txtPass.getEchoChar();
        chkShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char echo = chkShow.isSelected() ? (char) 0 : defaultEcho;
                txtPass.setEchoChar(echo);
                txtConfirmPass.setEchoChar(echo);
            }
        });

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

    private void doSignUp() {
        String fullName = txtFullName.getText().trim();
        String user = txtUser.getText().trim();
        char[] pass = txtPass.getPassword();
        char[] confirmPass = txtConfirmPass.getPassword();
        Role role = (Role) cmbRole.getSelectedItem();

        if (fullName.isEmpty()) {
            lblError.setText("Please enter your full name.");
            txtFullName.requestFocusInWindow();
            return;
        }
        if (user.isEmpty()) {
            lblError.setText("Please enter a username.");
            txtUser.requestFocusInWindow();
            return;
        }
        if (pass.length == 0) {
            lblError.setText("Please enter a password.");
            txtPass.requestFocusInWindow();
            return;
        }
        if (!Arrays.equals(pass, confirmPass)) {
            lblError.setText("Passwords do not match!");
            txtConfirmPass.setText("");
            txtConfirmPass.requestFocusInWindow();
            return;
        }

        boolean ok = auth.register(fullName, user, pass, role);
        Arrays.fill(pass, '\0');
        Arrays.fill(confirmPass, '\0');

        if (!ok) {
            String err = auth.getLastError();
            lblError.setText(err != null ? err : "Registration failed.");
            return;
        }

        lblError.setText(" ");
        JOptionPane.showMessageDialog(this,
                "Account created successfully for " + fullName + " (" + role + ")!\nYou can now log in.",
                "Registration Successful",
                JOptionPane.INFORMATION_MESSAGE);

        new LoginFrame(user).setVisible(true);
        dispose();
    }
}
