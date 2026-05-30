package its.view.javafx;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.Status;
import its.model.User;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainView {

    private final IssueController issueController;
    private final ProjectController projectController;
    private final AuthController authController;
    private final UserController userController;
    private final User currentUser;
    private final List<Project> projects = new ArrayList<>();

    private Stage stage;
    private ObservableList<IssueRow> allRows;
    private FilteredList<IssueRow> filteredRows;
    private ComboBox<ProjectItem> projectFilterBox;

    public MainView(IssueController issueController, ProjectController projectController,
                    AuthController authController, UserController userController,
                    User currentUser) {
        this.issueController = issueController;
        this.projectController = projectController;
        this.authController = authController;
        this.userController = userController;
        this.currentUser = currentUser;
    }

    public void show(Stage stage) {
        this.stage = stage;
        stage.setTitle("Issue Tracker");
        stage.setScene(buildScene());
        stage.setWidth(900);
        stage.setHeight(600);
        stage.show();
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setTop(buildTopBar());
        root.setCenter(buildTablePanel());
        return new Scene(root, 900, 600);
    }

    private HBox buildTopBar() {
        Label title = new Label("Issue Tracker");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        projectFilterBox = new ComboBox<>();
        projectFilterBox.setOnAction(e -> applyProjectFilter());

        HBox centerBox = new HBox(8, new Label("Project"), projectFilterBox);
        centerBox.setAlignment(Pos.CENTER);

        Button reportBtn = new Button("+ Report Issue");
        Button logoutBtn = new Button("Logout");
        reportBtn.setOnAction(e -> onReportIssue());
        logoutBtn.setOnAction(e -> onLogout());

        HBox rightBox = new HBox(8, reportBtn, logoutBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox();
        HBox.setHgrow(centerBox, javafx.scene.layout.Priority.ALWAYS);
        bar.getChildren().addAll(title, centerBox, rightBox);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private VBox buildTablePanel() {
        allRows = FXCollections.observableArrayList();
        filteredRows = new FilteredList<>(allRows, row -> true);

        TableView<IssueRow> table = new TableView<>(filteredRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        TableColumn<IssueRow, String> projectCol = new TableColumn<>("Project");
        projectCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().projectName));
        projectCol.setPrefWidth(120);

        TableColumn<IssueRow, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().issueId));
        idCol.setPrefWidth(40);

        TableColumn<IssueRow, String> nameCol = new TableColumn<>("Issue Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title));
        nameCol.setPrefWidth(200);

        TableColumn<IssueRow, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().priority != null ? d.getValue().priority.name() : ""));
        priorityCol.setPrefWidth(80);

        TableColumn<IssueRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().status != null ? d.getValue().status.name() : ""));
        statusCol.setPrefWidth(90);

        TableColumn<IssueRow, String> reporterCol = new TableColumn<>("Reporter");
        reporterCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reporter));
        reporterCol.setPrefWidth(90);

        TableColumn<IssueRow, String> assigneeCol = new TableColumn<>("Assignee");
        assigneeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().assignee));
        assigneeCol.setPrefWidth(90);

        TableColumn<IssueRow, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(50);
        actionCol.setMaxWidth(60);
        actionCol.setCellFactory(new ActionButtonCellFactory(currentUser, this::refreshTable));

        table.getColumns().addAll(
                projectCol, idCol, nameCol, priorityCol, statusCol, reporterCol, assigneeCol, actionCol);

        loadProjects();
        loadIssues();
        refreshProjectFilter();

        VBox panel = new VBox(table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return panel;
    }

    private void loadProjects() {
        projects.clear();
        projects.addAll(projectController.getAllProjects());
        List<Issue> allIssues = issueController.getAllIssues();
        for (Project project : projects) {
            project.getIssues().clear();
            for (long issueId : project.getIssueIds()) {
                allIssues.stream()
                        .filter(i -> i.getIssueId() == issueId)
                        .findFirst()
                        .ifPresent(project::addIssue);
            }
        }
    }

    private void loadIssues() {
        allRows.clear();
        for (Project project : projects)
            for (Issue issue : project.getIssues())
                allRows.add(new IssueRow(project, issue));
    }

    private void refreshProjectFilter() {
        projectFilterBox.getItems().clear();
        projectFilterBox.getItems().add(ProjectItem.all());
        for (Project p : projects) projectFilterBox.getItems().add(ProjectItem.of(p));
        projectFilterBox.getSelectionModel().selectFirst();
    }

    private void applyProjectFilter() {
        ProjectItem selected = projectFilterBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isAll()) {
            filteredRows.setPredicate(row -> true);
        } else {
            long projectId = selected.project.getProjectId();
            filteredRows.setPredicate(row -> row.projectId == projectId);
        }
    }

    private void onReportIssue() {
        ProjectItem selected = projectFilterBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isAll()) {
            new Alert(Alert.AlertType.WARNING,
                    "Select a project before reporting an issue.").showAndWait();
            return;
        }
        new ReportIssueView(stage, issueController, currentUser,
                selected.project, this::refreshTable).show();
    }

    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Logout?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) new LoginView(authController, issueController,
                        userController, projectController).show(stage);
        });
    }

    public void refreshTable() {
        loadProjects();
        loadIssues();
        refreshProjectFilter();
        applyProjectFilter();
    }

    // ── 내부 데이터 모델 ──────────────────────────
    static class IssueRow {
        final long projectId;
        final String projectName;
        final long issueId;
        final String title;
        final Priority priority;
        final Status status;
        final String reporter;
        final String assignee;
        final Project project;
        final Issue issue;

        IssueRow(Project project, Issue issue) {
            this.project = project;
            this.issue = issue;
            this.projectId = project.getProjectId();
            this.projectName = project.getName();
            this.issueId = issue.getIssueId() != 0 ? issue.getIssueId() : 0;
            this.title = issue.getTitle();
            this.priority = issue.getPriority();
            this.status = issue.getStatus();
            this.reporter = issue.getReporter() != null ? issue.getReporter().getLoginId() : "-";
            this.assignee = issue.getAssignee() != null ? issue.getAssignee().getLoginId() : "-";
        }
    }

    static class ProjectItem {
        final Project project;
        ProjectItem(Project p) { this.project = p; }
        static ProjectItem all() { return new ProjectItem(null); }
        static ProjectItem of(Project p) { return new ProjectItem(p); }
        boolean isAll() { return project == null; }
        @Override public String toString() { return isAll() ? "All Projects" : project.getName(); }
    }
}