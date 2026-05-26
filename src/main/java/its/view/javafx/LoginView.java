package its.view.javafx;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.Project;
import its.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class LoginView {

    private final AuthController authController;
    private final IssueController issueController;
    private final ProjectController projectController;
    private final UserController userController;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Stage stage;

    public LoginView() {
        this(new AuthController(), new IssueController(), new UserController(), new ProjectController());
    }

    public LoginView(AuthController authController, IssueController issueController,
                     UserController userController, ProjectController projectController) {
        this.authController = authController;
        this.issueController = issueController;
        this.userController = userController;
        this.projectController = projectController;
    }

    public void show(Stage stage) {
        this.stage = stage;
        stage.setTitle("Issue Tracker Login");
        stage.setScene(buildScene());
        stage.setResizable(false);
        stage.show();
    }

    private Scene buildScene() {
        usernameField = new TextField();
        usernameField.setPrefWidth(200);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(200);
        errorLabel = new Label(" ");
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setAlignment(Pos.CENTER);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.CENTER);
        form.add(new Label("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");
        loginBtn.setPrefWidth(80);
        registerBtn.setPrefWidth(80);

        HBox btnBox = new HBox(8, loginBtn, registerBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, form, btnBox, errorLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 20, 20, 20));

        loginBtn.setOnAction(e -> onLogin());
        registerBtn.setOnAction(e -> onRegister());
        passwordField.setOnAction(e -> onLogin());

        return new Scene(root, 360, 230);
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            errorLabel.setText("Username을 입력해주세요.");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Password를 입력해주세요.");
            passwordField.requestFocus();
            return;
        }

        try {
            User user = authController.login(username, password);
            List<Project> projects = projectController.getAllProjects();
            List<User> allUsers = userController.findAllUsers(user);
            errorLabel.setText(" ");

            if (user.isAdmin()) {
                new AdminView(authController, projectController, issueController, userController,
                        user, projects, allUsers).show(stage);
            } else {
                new MainView(issueController, projectController, authController, userController,
                        user).show(stage);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            String msg = e.getMessage();
            errorLabel.setText("Account is not active.".equals(msg)
                    ? "계정이 활성화되지 않았습니다. 관리자에게 문의하세요." : msg);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    private void onRegister() {
        try {
            authController.register(usernameField.getText(), passwordField.getText());
            errorLabel.setText(" ");
            passwordField.clear();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Register");
            alert.setContentText("Registration submitted. Please wait for admin approval.");
            alert.showAndWait();
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
}