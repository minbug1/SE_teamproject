package its;

import its.controller.AuthController;
import its.controller.CategoryController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.StatisticsController;
import its.controller.UserController;
import its.repository.CategoryRepository;
import its.repository.FileCategoryRepository;
import its.repository.FileIssueRepository;
import its.repository.FileProjectRepository;
import its.repository.FileUserRepository;
import its.repository.IssueRepository;
import its.repository.ProjectRepository;
import its.repository.UserRepository;
import its.view.javafx.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainFX extends Application {

    static final String USER_FILE    = "data/test_users.json";
    static final String ISSUE_FILE   = "data/test_issues.json";
    static final String PROJECT_FILE = "data/test_projects.json";

    @Override
    public void start(Stage primaryStage) {
        UserRepository userRepository = new FileUserRepository(USER_FILE);
        IssueRepository issueRepository = new FileIssueRepository(ISSUE_FILE, userRepository);
        ProjectRepository projectRepository = new FileProjectRepository(PROJECT_FILE, userRepository, issueRepository);
        CategoryRepository categoryRepository = new FileCategoryRepository(issueRepository);

        IssueController issueController = new IssueController(issueRepository, projectRepository);
        AuthController authController = new AuthController(userRepository);
        UserController userController = new UserController(userRepository);
        ProjectController projectController = new ProjectController(projectRepository);
        StatisticsController statisticsController = new StatisticsController(issueRepository);
        CategoryController categoryController = new CategoryController(issueRepository, categoryRepository);

        new LoginView(authController, issueController, userController, projectController,
                statisticsController, categoryController)
                .show(primaryStage);
    }

    // public static void main(String[] args) {
    //     launch(args);
    // }
}
