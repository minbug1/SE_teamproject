package its.view.javafx;

import its.controller.IssueController;
import its.model.DeveloperRecommendation;
import its.model.Priority;
import its.model.IssueStatus;
import its.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class ActionButtonCell extends TableCell<MainView.IssueRow, Void> {

    private static final String ACTION_MENU_TEXT = "\u22EF";

    private final User currentUser;
    private final IssueController issueController;
    private final Runnable refreshCallback;
    private final Button button = new Button(ACTION_MENU_TEXT);

    public ActionButtonCell(User currentUser, IssueController issueController, Runnable refreshCallback) {
        this.currentUser = currentUser;
        this.issueController = issueController;
        this.refreshCallback = refreshCallback;
        button.setOnAction(e -> showActionMenu());
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty || getIndex() < 0 ? null : button);
    }

    private MainView.IssueRow currentRow() {
        return getTableView().getItems().get(getIndex());
    }

    private void showActionMenu() {
        MainView.IssueRow row = currentRow();
        IssueStatus status = row.status;
        ContextMenu menu = new ContextMenu();

        addItem(menu, "상세 보기",  () -> showIssueDetail(row));
        addItem(menu, "코멘트 추가", () -> addComment(row));

        if (currentUser != null && currentUser.isAdmin()) {
            addItem(menu, "이슈 삭제", () -> deleteIssue(row));
        }

        if (currentUser != null && currentUser.isPL()) {
            if (status == IssueStatus.NEW || status == IssueStatus.REOPENED)
                addItem(menu, "담당자 지정", () -> changeAssignee(row));
            addItem(menu, "우선순위 변경", () -> changePriority(row));
            if (status == IssueStatus.RESOLVED)
                addItem(menu, "이슈 닫기", () -> updateStatus(row, IssueStatus.CLOSED));
        }

        if (currentUser != null && currentUser.isDev()
                && status == IssueStatus.ASSIGNED && isCurrentUser(row.assignee))
            addItem(menu, "수정 완료", () -> updateStatus(row, IssueStatus.FIXED));

        if (currentUser != null && currentUser.isTester()
                && status == IssueStatus.FIXED && isCurrentUser(row.reporter)) {
            addItem(menu, "검증 통과", () -> updateStatus(row, IssueStatus.RESOLVED));
            addItem(menu, "재오픈",    () -> updateStatus(row, IssueStatus.REOPENED));
        }

        menu.show(button, Side.BOTTOM, 0, 0);
    }

    private void addItem(ContextMenu menu, String label, Runnable action) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> action.run());
        menu.getItems().add(item);
    }

    // ── 상세 보기 ─────────────────────────────────
    private void showIssueDetail(MainView.IssueRow row) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project : ").append(row.projectName)
          .append("\nID      : ").append(row.issueId)
          .append("\nTitle   : ").append(row.title)
          .append("\nPriority: ").append(row.priority != null ? row.priority.name() : "-")
          .append("\nStatus  : ").append(row.status   != null ? row.status.name()   : "-")
          .append("\nReporter: ").append(row.reporter)
          .append("\nAssignee: ").append(row.assignee);

        if (row.issue != null && !row.issue.getComments().isEmpty()) {
            sb.append("\n\n── 코멘트 ──");
            row.issue.getComments().forEach(c ->
                sb.append("\n[")
                  .append(c.getAuthor() != null ? c.getAuthor().getLoginId() : "?")
                  .append("] ")
                  .append(c.getContent()));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Issue Detail");
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // ── 코멘트 추가 ───────────────────────────────
    private void addComment(MainView.IssueRow row) {
        TextArea area = new TextArea();
        area.setPromptText("코멘트를 입력하세요.");
        area.setWrapText(true);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("코멘트 추가");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(area);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String content = area.getText().trim();
            if (content.isEmpty()) return;
            try {
                issueController.addComment(row.project, row.issueId, content, currentUser);
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 이슈 삭제 ─────────────────────────────────
    private void deleteIssue(MainView.IssueRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "이슈 #" + row.issueId + " 를 삭제하시겠습니까?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("이슈 삭제");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            try {
                issueController.deleteIssue(row.project, row.issueId, currentUser);
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 담당자 지정 (PL 전용) ─────────────────────
    private void changeAssignee(MainView.IssueRow row) {
        List<User> candidates = row.project.getMembers().stream()
                .filter(User::isDev)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "배정 가능한 개발자가 없습니다.").showAndWait();
            return;
        }

        ComboBox<User> assigneeCombo = new ComboBox<>();
        assigneeCombo.getItems().addAll(candidates);
        assigneeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getLoginId());
            }
        });
        assigneeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getLoginId());
            }
        });
        assigneeCombo.getSelectionModel().selectFirst();

        VBox leftPanel = new VBox(6, new Label("담당자"), assigneeCombo);
        leftPanel.setPadding(new Insets(8));
        leftPanel.setPrefWidth(180);

        TableView<DeveloperRecommendation> recTable = buildRecommendationTable(row, assigneeCombo);
        VBox rightPanel = new VBox(6, new Label("담당자 추천"), recTable);
        rightPanel.setPadding(new Insets(8));
        VBox.setVgrow(recTable, javafx.scene.layout.Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.32);
        splitPane.setPrefSize(620, 280);

        Dialog<ButtonType> assignDialog = new Dialog<>();
        assignDialog.setTitle("담당자 지정");
        assignDialog.getDialogPane().setContent(splitPane);
        assignDialog.getDialogPane().setPrefSize(640, 340);
        assignDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        assignDialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            User assignee = assigneeCombo.getSelectionModel().getSelectedItem();
            if (assignee == null) return;

            // ── 담당자 확정 후 코멘트 입력 ──────────
            TextArea commentArea = new TextArea();
            commentArea.setPromptText("코멘트 입력 (선택)");
            commentArea.setWrapText(true);
            commentArea.setPrefRowCount(4);
            commentArea.setPrefWidth(300);

            Dialog<ButtonType> commentDialog = new Dialog<>();
            commentDialog.setTitle("코멘트 입력");
            commentDialog.setHeaderText(assignee.getLoginId() + " 님을 담당자로 지정합니다.");
            commentDialog.getDialogPane().setContent(commentArea);
            commentDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            commentDialog.showAndWait().ifPresent(cbt -> {
                if (cbt != ButtonType.OK) return;  // 취소 시 담당자 지정도 취소
                String comment = commentArea.getText().trim().isEmpty()
                        ? null : commentArea.getText().trim();
                try {
                    issueController.assignIssue(row.project, row.issueId, assignee, currentUser, comment);
                    refresh();
                } catch (Exception ex) { showError(ex.getMessage()); }
            });
        });
    }

    // ── 담당자 추천 테이블 ────────────────────────
    private TableView<DeveloperRecommendation> buildRecommendationTable(
            MainView.IssueRow row, ComboBox<User> assigneeCombo) {

        TableView<DeveloperRecommendation> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(200);

        TableColumn<DeveloperRecommendation, String> devCol = new TableColumn<>("Developer");
        devCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDeveloper() != null
                        ? d.getValue().getDeveloper().getLoginId() : "-"));

        TableColumn<DeveloperRecommendation, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.3f", d.getValue().getScore())));

        TableColumn<DeveloperRecommendation, Number> matchedCol = new TableColumn<>("Matched");
        matchedCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getMatchedIssueCount()));

        TableColumn<DeveloperRecommendation, Number> solvedCol = new TableColumn<>("Solved");
        solvedCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getTotalSolvedIssueCount()));

        table.getColumns().addAll(devCol, scoreCol, matchedCol, solvedCol);

        try {
            List<DeveloperRecommendation> recs =
                    issueController.recommendAssignees(row.project, row.issueId, currentUser);
            table.getItems().addAll(recs);
        } catch (Exception ex) {
            table.setPlaceholder(new Label("추천 로드 실패: " + ex.getMessage()));
        }

        if (table.getItems().isEmpty()) {
            table.setPlaceholder(new Label("추천 없음 (이슈 분류 후 사용 가능)"));
        }

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getDeveloper() == null) return;
            assigneeCombo.getItems().stream()
                    .filter(u -> u.getLoginId().equals(newVal.getDeveloper().getLoginId()))
                    .findFirst()
                    .ifPresent(u -> assigneeCombo.getSelectionModel().select(u));
        });

        return table;
    }

    // ── 우선순위 변경 (PL 전용) ───────────────────
    private void changePriority(MainView.IssueRow row) {
        ComboBox<Priority> combo = new ComboBox<>();
        combo.getItems().addAll(Priority.values());
        combo.setValue(row.priority != null ? row.priority : Priority.MAJOR);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("우선순위 변경");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(combo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Priority selected = combo.getValue();
            if (selected == null) return;
            try {
                issueController.changePriority(row.project, row.issueId, selected, currentUser);
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 상태 변경 ─────────────────────────────────
    private void updateStatus(MainView.IssueRow row, IssueStatus status) {
        try {
            switch (status) {
                case FIXED:
                    issueController.fixIssue(row.project, row.issueId, null, currentUser);
                    break;
                case RESOLVED:
                    issueController.verifyIssue(row.project, row.issueId, null, currentUser, true);
                    break;
                case REOPENED:
                    issueController.verifyIssue(row.project, row.issueId, null, currentUser, false);
                    break;
                case CLOSED:
                    issueController.closeIssue(row.project, row.issueId, null, currentUser);
                    break;
                default:
                    break;
            }
            refresh();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // ── 공통 유틸 ─────────────────────────────────
    private boolean isCurrentUser(String loginId) {
        return currentUser != null && loginId != null
                && currentUser.getLoginId().equals(loginId);
    }

    private void refresh() {
        if (refreshCallback != null) refreshCallback.run();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}