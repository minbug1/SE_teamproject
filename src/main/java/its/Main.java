package its;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import its.controller.AuthController;
import its.controller.IssueController;
// import its.model.AccountStatus;
// import its.model.Role;
// import its.model.User;
import its.view.swing.LoginView;

public class Main {

    // 테스트용 파일 경로 (실제 운영 데이터와 분리)
    static final String USER_FILE    = "data/test_users.json";
    static final String ISSUE_FILE   = "data/test_issues.json";
    static final String PROJECT_FILE = "data/test_projects.json";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Failed to set look and feel: " + e.getMessage());
            }

            IssueController issueController = new IssueController();
            AuthController authController = new AuthController();
            // User debugUser = new User(
            //         1L,
            //         "debug-tester",
            //         "password",
            //         AccountStatus.ACTIVE,
            //         Role.TESTER
            // );

            new LoginView(authController, issueController).setVisible(true);
        });
    }
}