package its.view.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import its.controller.AuthController;
import its.controller.IssueController;
import its.model.User;

public class LoginView extends JFrame {

    private final AuthController authController;
    private final IssueController issueController;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    public LoginView() {
        this(new AuthController(), new IssueController());
    }

    public LoginView(AuthController authController, IssueController issueController) {
        this.authController = authController;
        this.issueController = issueController;
        initUI();
    }

    private void initUI() {
        setTitle("Issue Tracker Login");
        setSize(360, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        JButton loginButton = new JButton("Login");
        errorLabel = new JLabel(" ");
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username"), gbc);

        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Password"), gbc);

        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        add(formPanel, BorderLayout.CENTER);
        add(errorLabel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> onLogin());
        getRootPane().setDefaultButton(loginButton);
    }

    private void onLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User user = authController.login(username, password);
            errorLabel.setText(" ");
            dispose();
            new MainView(issueController, user).setVisible(true);
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    public void open() {
        setVisible(true);
    }
}
