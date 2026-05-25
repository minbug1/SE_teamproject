package its;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.repository.FileIssueRepository;
import its.repository.FileProjectRepository;
import its.repository.FileUserRepository;
import its.repository.IssueRepository;
import its.repository.ProjectRepository;
import its.repository.UserRepository;
// import its.model.AccountStatus;
// import its.model.Role;
// import its.model.User;
import its.view.swing.LoginView;

public class Main {

    // 테스트용 파일 경로
    static final String USER_FILE    = "data/test_users.json";
    static final String ISSUE_FILE   = "data/test_issues.json";
    static final String PROJECT_FILE = "data/test_projects.json";

    // 실제 파일 경로
    // static final String USER_FILE    = "data/users.json";
    // static final String ISSUE_FILE   = "data/issues.json";
    // static final String PROJECT_FILE = "data/projects.json";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Failed to set look and feel: " + e.getMessage());
            }

            UserRepository userRepository = new FileUserRepository(USER_FILE);
            IssueRepository issueRepository = new FileIssueRepository(ISSUE_FILE, userRepository);
            ProjectRepository projectRepository = new FileProjectRepository(PROJECT_FILE, userRepository, issueRepository);

            IssueController issueController = new IssueController(issueRepository, projectRepository);
            AuthController authController = new AuthController(userRepository);
            UserController userController = new UserController(userRepository);
            ProjectController projectController = new ProjectController(projectRepository);

            new LoginView(authController, issueController, userController, projectController).setVisible(true);
        });
    }
}
