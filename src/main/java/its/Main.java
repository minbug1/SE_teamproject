package its;

import javax.security.auth.login.LoginContext;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import its.controller.AuthController;
import its.controller.IssueController;
import its.model.AccountStatus;
import its.model.Role;
import its.model.User;
import its.view.swing.LoginView;
import its.view.swing.MainView;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Failed to set look and feel: " + e.getMessage());
            }

            IssueController issueController = new IssueController();
            AuthController authController = new AuthController();
            User debugUser = new User(
                    1L,
                    "debug-tester",
                    "password",
                    AccountStatus.ACTIVE,
                    Role.TESTER
            );

            new LoginView(authController, issueController).setVisible(true);
        });
    }
}
