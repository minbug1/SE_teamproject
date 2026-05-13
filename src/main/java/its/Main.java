package its;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import its.controller.IssueController;
import its.model.AccountStatus;
import its.model.Role;
import its.model.User;
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
            User debugUser = new User(
                    1L,
                    "debug-tester",
                    "password",
                    AccountStatus.ACTIVE,
                    Role.TESTER
            );

            new MainView(issueController, debugUser).setVisible(true);
        });
    }
}
