package its.view.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.Project;
import its.model.User;

public class LoginView extends JFrame {

    private final AuthController authController;
    private final IssueController issueController;
    private final ProjectController projectController;
    private final UserController userController;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    public LoginView() {
        this(new AuthController(), new IssueController(), new UserController(), new ProjectController());
    }

    public LoginView(AuthController authController, IssueController issueController, UserController userController, ProjectController projectController) {
        this.authController = authController;
        this.issueController = issueController;
        this.userController = userController;
        this.projectController = projectController;
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
        JButton registerButton = new JButton("Register");
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
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.CENTER);
        add(errorLabel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> onLogin());
        registerButton.addActionListener(e -> onRegister());
        getRootPane().setDefaultButton(loginButton);
    }

    private void onLogin() {
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());

    // View 단 빈칸 선검증
    if (username.isEmpty()) {
        errorLabel.setText("Username을 입력해주세요.");
        return;
    }
    if (password.isEmpty()) {
        errorLabel.setText("Password를 입력해주세요.");
        passwordField.requestFocusInWindow();
        return;
    }

    try {
        User user = authController.login(username, password);
        List<Project> projects = projectController.getAllProjects();
        List<User> allUsers = userController.findAllUsers(user);
        errorLabel.setText(" ");
        dispose();
        if (user.isAdmin()) {
            new AdminView(authController, projectController, issueController, userController,
                          user, projects, allUsers).setVisible(true);
        } else {
            new MainView(authController, issueController, projectController, userController, user).setVisible(true);
        }
    } catch (IllegalArgumentException | IllegalStateException e) {
        String msg = e.getMessage();
        if ("Account is not active.".equals(msg)) {
            errorLabel.setText("계정이 활성화되지 않았습니다. 관리자에게 문의하세요.");
        } else {
            errorLabel.setText(msg);
        }
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }
}

    private void onRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            authController.register(username, password);
            errorLabel.setText(" ");
            passwordField.setText("");
            JOptionPane.showMessageDialog(
                    this,
                    "Registration submitted. Please wait for admin approval.",
                    "Register",
                    JOptionPane.INFORMATION_MESSAGE);
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
