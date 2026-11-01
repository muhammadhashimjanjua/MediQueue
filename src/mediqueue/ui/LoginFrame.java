package mediqueue.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import mediqueue.auth.AuthService;
import mediqueue.auth.AuthService.Role;

/** MediQueue login window (Swing). */
public class LoginFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x0F766E);
    private static final Color PRIMARY_DARK = new Color(0x115E59);
    private static final Color ERROR = new Color(0xB91C1C);

    private final AuthService auth = new AuthService();

    private final JTextField txtUser = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();
    private final JComboBox<Role> cmbRole = new JComboBox<Role>(Role.values());
    private final JCheckBox chkShow = new JCheckBox("Show password");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = new JButton("Log In");
    private final JButton btnGoSignUp = new JButton("Don't have an account? Sign Up");

    public LoginFrame() {
        this("");
    }

    public LoginFrame(String initialUsername) {
        super("MediQueue - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(btnLogin);   // Enter key = Log In
        pack();
        setSize(760, 480);
        setResizable(false);
        setLocationRelativeTo(null);

        if (initialUsername != null && !initialUsername.trim().isEmpty()) {
            txtUser.setText(initialUsername.trim());
            txtPass.requestFocusInWindow();
        }
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
        c.gridx = 0; c.gridy = GridBagConstraints.RELATIVE; c.insets = new Insets(4, 0, 4, 0);
        p.add(logo, c); p.add(name, c); p.add(tag, c);
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 36, 20, 36));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST; c.insets = new Insets(3, 0, 3, 0);

        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel sub = new JLabel("Sign in to continue");
        sub.setForeground(Color.GRAY);

        Dimension field = new Dimension(100, 30);
        txtUser.setPreferredSize(field);
        txtPass.setPreferredSize(field);
        cmbRole.setPreferredSize(field);
        cmbRole.setBackground(Color.WHITE);
        chkShow.setBackground(Color.WHITE);
        chkShow.setFocusPainted(false);

        lblError.setForeground(ERROR);
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 11));

        btnLogin.setBackground(PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setFocusPainted(false);
        btnLogin.setBorder(new EmptyBorder(9, 0, 9, 0));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnLogin.setBackground(PRIMARY_DARK); }
            @Override public void mouseExited(MouseEvent e)  { btnLogin.setBackground(PRIMARY); }
        });

        btnGoSignUp.setForeground(PRIMARY);
        btnGoSignUp.setBackground(Color.WHITE);
        btnGoSignUp.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnGoSignUp.setBorderPainted(false);
        btnGoSignUp.setContentAreaFilled(false);
        btnGoSignUp.setFocusPainted(false);
        btnGoSignUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel hint = new JLabel("<html><span style='color:#6B7280;font-size:10px'>Demo: doctor / doctor123 (role Doctor)</span></html>");

        int row = 0;
        c.gridy = row++; p.add(title, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0); p.add(sub, c);
        c.insets = new Insets(2, 0, 2, 0);
        c.gridy = row++; p.add(new JLabel("Role"), c);
        c.gridy = row++; p.add(cmbRole, c);
        c.gridy = row++; p.add(new JLabel("Username"), c);
        c.gridy = row++; p.add(txtUser, c);
        c.gridy = row++; p.add(new JLabel("Password"), c);
        c.gridy = row++; p.add(txtPass, c);
        c.gridy = row++; p.add(chkShow, c);
        c.gridy = row++; p.add(lblError, c);
        c.gridy = row++; p.add(btnLogin, c);
        c.gridy = row++; c.insets = new Insets(4, 0, 0, 0); p.add(btnGoSignUp, c);
        c.gridy = row++; c.insets = new Insets(4, 0, 0, 0); p.add(hint, c);

        final char defaultEcho = txtPass.getEchoChar();
        chkShow.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                txtPass.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho);
            }
        });
        btnLogin.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doLogin(); }
        });
        btnGoSignUp.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                new SignUpFrame().setVisible(true);
                dispose();
            }
        });
        return p;
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        char[] pass = txtPass.getPassword();
        if (user.isEmpty() || pass.length == 0) {
            lblError.setText("Please enter your username and password.");
            return;
        }
        Role role = (Role) cmbRole.getSelectedItem();
        String fullName = auth.login(user, pass, role);
        java.util.Arrays.fill(pass, '\0');          // don't keep the password in memory

        if (fullName == null) {
            String err = auth.getLastError();
            lblError.setText(err != null ? err : "Invalid username, password or role.");
            txtPass.setText("");
            txtPass.requestFocusInWindow();
            return;
        }
        lblError.setText(" ");
        new DashboardFrame(fullName, role).setVisible(true);
        dispose();
    }
}
