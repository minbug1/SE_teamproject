package its.view.javafx;

import its.controller.IssueController;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ReportIssueView {

    private final Stage parentStage;
    private final IssueController issueController;
    private final User currentUser;
    private final Project currentProject;
    private final Runnable refreshCallback;

    private Stage stage;
    private TextField titleField;
    private TextArea descArea;
    private ComboBox<Priority> priorityBox;
    private TextArea commentArea;

    public ReportIssueView(Stage parentStage, IssueController issueController,
                           User currentUser, Project currentProject, Runnable refreshCallback) {
        this.parentStage = parentStage;
        this.issueController = issueController;
        this.currentUser = currentUser;
        this.currentProject = currentProject;
        this.refreshCallback = refreshCallback;
    }

    public void show() {
        stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("새 이슈 등록");
        stage.setResizable(false);
        stage.setScene(buildScene());
        stage.showAndWait();
    }

    private Scene buildScene() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("기본 정보", buildBasicInfoPanel()),
                new Tab("코멘트",   buildCommentPanel())
        );

        Button cancelBtn = new Button("취소");
        Button submitBtn = new Button("등록");
        submitBtn.setStyle("-fx-background-color: #4682B4; -fx-text-fill: white;");
        submitBtn.setDefaultButton(true);
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> onCancel());
        submitBtn.setOnAction(e -> onSubmit());

        HBox btnBox = new HBox(8, cancelBtn, submitBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(0, 20, 15, 20));

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(btnBox);

        return new Scene(root, 500, 420);
    }

    private GridPane buildBasicInfoPanel() {
        titleField = new TextField();
        descArea   = new TextArea();
        descArea.setWrapText(true);
        descArea.setPrefRowCount(5);

        priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(Priority.values());
        priorityBox.setValue(Priority.MAJOR);
        priorityBox.setMaxWidth(Double.MAX_VALUE);

        TextField reporterField = new TextField(String.valueOf(currentUser.getUserId()));
        reporterField.setEditable(false);
        reporterField.setStyle("-fx-background-color: #d3d3d3;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16, 20, 10, 20));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(80);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        grid.add(new Label("제목 *"),   0, 0); grid.add(titleField,  1, 0);
        grid.add(new Label("내용"),     0, 1); grid.add(new ScrollPane(descArea), 1, 1);
        grid.add(new Label("우선순위 *"), 0, 2); grid.add(priorityBox, 1, 2);
        grid.add(new Label("Reporter"), 0, 3); grid.add(reporterField, 1, 3);

        GridPane.setHgrow(titleField,    javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(priorityBox,   javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(reporterField, javafx.scene.layout.Priority.ALWAYS);
        return grid;
    }

    private VBox buildCommentPanel() {
        Label guide = new Label("이슈 등록 시 추가할 코멘트를 입력하세요. (선택)");
        guide.setStyle("-fx-text-fill: gray;");

        commentArea = new TextArea();
        commentArea.setWrapText(true);
        VBox.setVgrow(commentArea, javafx.scene.layout.Priority.ALWAYS);

        VBox panel = new VBox(8, guide, commentArea);
        panel.setPadding(new Insets(16, 20, 10, 20));
        return panel;
    }

    private void onSubmit() {
        String title   = titleField.getText().trim();
        String desc    = descArea.getText().trim();
        Priority priority = priorityBox.getValue();
        String comment = commentArea.getText().trim();

        if (title.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "제목을 입력해주세요.", ButtonType.OK).showAndWait();
            titleField.requestFocus();
            return;
        }

        try {
            Issue created = issueController.reportIssue(
                    currentProject, title, desc, currentUser, priority, comment);
            new Alert(Alert.AlertType.INFORMATION,
                    "이슈 #" + created.getIssueId() + " 가 등록되었습니다.", ButtonType.OK).showAndWait();
            if (refreshCallback != null) refreshCallback.run();
            stage.close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "이슈 등록 실패: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void onCancel() {
        boolean hasInput = !titleField.getText().trim().isEmpty()
                || !descArea.getText().trim().isEmpty()
                || !commentArea.getText().trim().isEmpty();
        if (hasInput) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "작성 중인 내용이 사라집니다. 취소하시겠습니까?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) stage.close(); });
        } else {
            stage.close();
        }
    }
}