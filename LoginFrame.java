import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Login window. Calls onSuccess with the authenticated user. */
class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(16);
    private final JPasswordField password = new JPasswordField(16);

    LoginFrame(AuthService auth, Consumer<User> onSuccess) {
        super("Park-O-Matic - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("<html><div style='text-align:center'><b style='font-size:18px'>PARK-O-MATIC</b><br>"
                + "Intelligent Parking Management</div></html>", SwingConstants.CENTER);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(title, c);
        c.gridwidth = 1;
        c.gridy = 1; c.gridx = 0; p.add(new JLabel("Username"), c);
        c.gridx = 1; p.add(username, c);
        c.gridy = 2; c.gridx = 0; p.add(new JLabel("Password"), c);
        c.gridx = 1; p.add(password, c);

        JButton login = new JButton("Log in");
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2; p.add(login, c);

        login.addActionListener(e -> {
            final User[] result = new User[1];
            boolean ok = Ui.attempt(this, () ->
                    result[0] = auth.login(username.getText(), new String(password.getPassword())));
            password.setText("");
            if (ok) {
                dispose();
                onSuccess.accept(result[0]);
            }
        });
        getRootPane().setDefaultButton(login);

        add(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }
}
