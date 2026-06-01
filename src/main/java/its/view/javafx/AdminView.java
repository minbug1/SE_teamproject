package its.view.javafx;

import its.controller.AuthController;
import its.controller.CategoryController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.StatisticsController;
import its.controller.UserController;
import its.model.AccountStatus;
import its.model.Project;
import its.model.User;
import its.model.Issue;
import its.model.UserRole;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AdminView {

    private static final String USERS_LABEL = "Users";

    private final AuthController authController;
    private final ProjectController projectController;
    private final IssueController issueController;
    private final UserController userController;
    private final StatisticsController statisticsController;
    private final CategoryController categoryController;
    private final User adminUser;
    private final List<Project> projects;
    private final List<User> allUsers;

    private Stage stage;
    private ListView<String> projectListView;
    private ObservableList<String> projectItems;
    private Label projectTitleLabel;
    private Label projectDescriptionLabel;
    private TableView<UserRow> memberTable;
    private TableView<UserRow> userTable;
    private ObservableList<UserRow> memberRows;
    private ObservableList<UserRow> userRows;
    private Project selectedProject;
    private boolean refreshing = false;

    public AdminView(AuthController authController, ProjectController projectController,
                     IssueController issueController, UserController userController,
                     StatisticsController statisticsController, CategoryController categoryController,
                     User adminUser, List<Project> projects, List<User> allUsers) {
        this.authController = authController;
        this.projectController = projectController;
        this.issueController = issueController;
        this.userController = userController;
        this.statisticsController = statisticsController;
        this.categoryController = categoryController;
        this.adminUser = adminUser;
        this.projects = projects;
        this.allUsers = allUsers;
    }

    public void show(Stage stage) {
        this.stage = stage;
        stage.setTitle("Issue Tracker Admin");
        stage.setScene(buildScene());
        stage.setWidth(940);
        stage.setHeight(620);
        stage.show();

        refreshProjectList();
        if (!projects.isEmpty()) projectListView.getSelectionModel().selectFirst();
        else showUsers();
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setTop(buildTopBar());
        root.setLeft(buildProjectListPanel());
        root.setCenter(buildMainPanel());
        return new Scene(root, 940, 620);
    }

    private HBox buildTopBar() {
        Label title = new Label("Issue Tracker Admin");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label userLabel = new Label(adminUser.getLoginId());
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> onLogout());

        HBox rightBox = new HBox(8, userLabel, logoutBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox();
        HBox.setHgrow(title, Priority.ALWAYS);
        bar.getChildren().addAll(title, rightBox);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private VBox buildProjectListPanel() {
        projectItems = FXCollections.observableArrayList();
        projectListView = new ListView<>(projectItems);
        VBox.setVgrow(projectListView, Priority.ALWAYS);

        projectListView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (!refreshing) onProjectSelectionChanged(newVal.intValue());
                });

        Button addBtn = new Button("+ Project");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> doAddProject());

        VBox panel = new VBox(4, new Label("Projects"), projectListView, addBtn);
        panel.setPadding(new Insets(8));
        panel.setPrefWidth(220);
        panel.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");
        return panel;
    }

    private VBox buildMainPanel() {
        projectTitleLabel = new Label("Project");
        projectTitleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        projectDescriptionLabel = new Label(" ");

        VBox titleBox = new VBox(2, projectTitleLabel, projectDescriptionLabel);

        Button editBtn    = new Button("Edit");
        Button deleteBtn  = new Button("Delete");
        Button addMemberBtn = new Button("+ Member");
        editBtn.setOnAction(e -> doEditProject());
        deleteBtn.setOnAction(e -> doDeleteProject());
        addMemberBtn.setOnAction(e -> doAddMember());

        HBox headerBar = new HBox(8);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        headerBar.getChildren().addAll(titleBox, editBtn, deleteBtn, addMemberBtn);
        headerBar.setPadding(new Insets(8));
        headerBar.setAlignment(Pos.CENTER_LEFT);

        memberRows = FXCollections.observableArrayList();
        memberTable = buildUserTable(memberRows, true);
        VBox memberBox = buildTableSection("Project Members", memberTable);

        userRows = FXCollections.observableArrayList();
        userTable = buildUserTable(userRows, false);
        VBox userBox = buildTableSection(USERS_LABEL, userTable);

        VBox tablesBox = new VBox(8, memberBox, userBox);
        tablesBox.setPadding(new Insets(0, 8, 8, 8));
        VBox.setVgrow(memberBox, Priority.ALWAYS);
        VBox.setVgrow(userBox, Priority.ALWAYS);

        VBox main = new VBox(headerBar, tablesBox);
        VBox.setVgrow(tablesBox, Priority.ALWAYS);
        return main;
    }

    private VBox buildTableSection(String title, TableView<UserRow> table) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(4, label, table);
        box.setPadding(new Insets(4));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private TableView<UserRow> buildUserTable(ObservableList<UserRow> rows, boolean isMemberTable) {
        TableView<UserRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<UserRow, String> loginIdCol = new TableColumn<>("Username");
        loginIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().user.getLoginId()));
        loginIdCol.setEditable(false);
        loginIdCol.setPrefWidth(150);

        TableColumn<UserRow, UserRole> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().user.getRole()));
        roleCol.setCellFactory(ComboBoxTableCell.forTableColumn(UserRole.values()));
        roleCol.setPrefWidth(120);
        roleCol.setOnEditCommit(event -> {
            if (refreshing) return;
            UserRow row = event.getRowValue();
            try {
                authController.changeRole(adminUser, row.user.getUserId(), event.getNewValue());
                row.user.setRole(event.getNewValue());
                refreshAfterUpdate();
            } catch (Exception ex) {
                showWarning(ex.getMessage());
                table.refresh();
            }
        });

        TableColumn<UserRow, AccountStatus> statusCol = new TableColumn<>("Account Status");
        statusCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().user.getAccountStatus()));
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn(AccountStatus.values()));
        statusCol.setPrefWidth(140);
        statusCol.setOnEditCommit(event -> {
            if (refreshing) return;
            UserRow row = event.getRowValue();
            try {
                authController.changeAccountStatus(adminUser, row.user.getUserId(), event.getNewValue());
                row.user.setAccountStatus(event.getNewValue());
                refreshAfterUpdate();
            } catch (Exception ex) {
                showWarning(ex.getMessage());
                table.refresh();
            }
        });

        String btnLabel = isMemberTable ? "Remove" : "Assign";
        TableColumn<UserRow, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(90);
        actionCol.setMaxWidth(100);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button(btnLabel);
            {
                btn.setOnAction(e -> {
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return;
                    UserRow row = getTableView().getItems().get(getIndex());
                    if (isMemberTable) doRemoveMember(row.user);
                    else doAssignToProject(row.user);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(loginIdCol, roleCol, statusCol, actionCol);
        return table;
    }

    private void onProjectSelectionChanged(int index) {
        if (index < 0 || index >= projectItems.size()) return;
        if (index < projects.size()) {
            selectedProject = projects.get(index);
            refreshProjectPanel();
        }
    }

    private void refreshProjectList() {
        refreshing = true;
        int prev = projectListView.getSelectionModel().getSelectedIndex();
        projectItems.clear();
        for (Project p : projects) projectItems.add(p.getName());
        if (prev >= 0 && prev < projectItems.size())
            projectListView.getSelectionModel().select(prev);
        refreshing = false;
    }

    private void refreshProjectPanel() {
        if (selectedProject == null) return;
        projectTitleLabel.setText(selectedProject.getName()
                + " (" + selectedProject.getMembers().size() + " members)");
        String desc = selectedProject.getDescription();
        projectDescriptionLabel.setText(desc == null || desc.isBlank() ? " " : desc);
        memberRows.clear();
        for (User u : selectedProject.getMembers()) memberRows.add(new UserRow(u));
        refreshUserTable();
    }

    private void showUsers() {
        selectedProject = null;
        projectTitleLabel.setText(USERS_LABEL);
        projectDescriptionLabel.setText("Pending users and project assignment.");
        memberRows.clear();
        refreshUserTable();
    }

    private void refreshUserTable() {
        userRows.clear();
        for (User u : getManagedUsers()) userRows.add(new UserRow(u));
    }

    private void refreshAfterUpdate() {
        refreshProjectList();
        if (selectedProject != null) refreshProjectPanel();
        else showUsers();
    }

    private List<User> getManagedUsers() {
        List<User> result = new ArrayList<>();
        for (User u : allUsers) if (!u.isAdmin()) result.add(u);
        return result;
    }

    private List<User> getUsersNotInSelectedProject() {
        List<User> result = new ArrayList<>();
        if (selectedProject == null) return result;
        for (User u : getManagedUsers())
            if (!selectedProject.getMembers().contains(u)) result.add(u);
        return result;
    }

    private List<Project> getProjectsWithoutUser(User user) {
        List<Project> result = new ArrayList<>();
        for (Project p : projects) if (!p.getMembers().contains(user)) result.add(p);
        return result;
    }

    private void doAddProject() {
        TextField nameField = new TextField();
        TextField descField = new TextField();
        GridPane grid = buildFormGrid();
        grid.add(new Label("Project name"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Description"),  0, 1); grid.add(descField, 1, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Project");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String name = nameField.getText().trim();
            if (name.isEmpty()) { showWarning("Project name is required."); return; }
            for (Project p : projects)
                if (p.getName().equals(name)) { showWarning("Project name already exists."); return; }
            Project p = projectController.createProject(name, descField.getText().trim(), adminUser);
            projects.add(p);
            refreshProjectList();
            projectListView.getSelectionModel().select(projects.size() - 1);
        });
    }

    private void doEditProject() {
        if (selectedProject == null) return;
        TextField nameField = new TextField(selectedProject.getName());
        TextField descField = new TextField(
                selectedProject.getDescription() == null ? "" : selectedProject.getDescription());
        GridPane grid = buildFormGrid();
        grid.add(new Label("Project name"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Description"),  0, 1); grid.add(descField, 1, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Project");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String name = nameField.getText().trim();
            if (name.isEmpty()) { showWarning("Project name is required."); return; }
            selectedProject.setName(name);
            selectedProject.setDescription(descField.getText().trim());
            projectController.updateProject(selectedProject, adminUser);
            refreshProjectList();
            refreshProjectPanel();
        });
    }

    private void doDeleteProject() {
        if (selectedProject == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete '" + selectedProject.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Project");
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;

            for (its.model.Issue issue : new ArrayList<>(selectedProject.getIssues())) {
                try {
                    issueController.deleteIssue(selectedProject, issue.getIssueId(), adminUser);
                } catch (Exception e) {
                    System.err.println("이슈 삭제 실패: " + e.getMessage());
                }
            }

            projectController.deleteProject(selectedProject.getProjectId(), adminUser);
            projects.remove(selectedProject);
            selectedProject = null;
            refreshProjectList();
            if (!projects.isEmpty()) projectListView.getSelectionModel().selectFirst();
            else showUsers();
        });
    }

    private void doAddMember() {
        if (selectedProject == null) return;
        List<User> candidates = getUsersNotInSelectedProject();
        if (candidates.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No available users.").showAndWait();
            return;
        }
        List<String> loginIds = new ArrayList<>();
        candidates.forEach(u -> loginIds.add(u.getLoginId()));

        ChoiceDialog<String> dialog = new ChoiceDialog<>(loginIds.get(0), loginIds);
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Select user");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(selected -> {
            for (User u : candidates) {
                if (u.getLoginId().equals(selected)) {
                    projectController.addMemberToProject(selectedProject, u, adminUser);
                    break;
                }
            }
            refreshProjectList();
            refreshProjectPanel();
        });
    }

    private void doRemoveMember(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove '" + user.getLoginId() + "' from this project?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Remove Member");
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            projectController.removeMemberFromProject(selectedProject, user, adminUser);
            refreshProjectList();
            refreshProjectPanel();
        });
    }

    private void doAssignToProject(User user) {
        if (projects.isEmpty()) { showWarning("There are no projects."); return; }
        List<Project> candidates = getProjectsWithoutUser(user);
        if (candidates.isEmpty()) { showWarning("This user is already assigned to every project."); return; }

        List<String> names = new ArrayList<>();
        candidates.forEach(p -> names.add(p.getName()));
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Assign User");
        dialog.setHeaderText("Assign '" + user.getLoginId() + "' to project");
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(selected -> {
            for (Project p : candidates) {
                if (p.getName().equals(selected)) {
                    projectController.addMemberToProject(p, user, adminUser);
                    break;
                }
            }
            refreshProjectList();
            if (selectedProject != null) refreshProjectPanel();
            else showUsers();
        });
    }

    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Logout?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) new LoginView(authController, issueController,
                        userController, projectController, statisticsController, categoryController).show(stage);
        });
    }

    private GridPane buildFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        return grid;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    static class UserRow {
        final User user;
        UserRow(User user) { this.user = user; }
    }
}
