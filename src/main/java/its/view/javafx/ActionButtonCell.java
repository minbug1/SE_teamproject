package its.view.javafx;

import its.model.Priority;
import its.model.Status;
import its.model.User;
import javafx.geometry.Side;
import javafx.scene.control.*;

/**
 * Swing의 ActionButtonEditor에 해당.
 * 각 이슈 행의 ⋯ 버튼을 렌더링하고, 클릭 시 역할별 ContextMenu를 표시.
 */
public class ActionButtonCell extends TableCell<MainView.IssueRow, Void> {

    private static final String ACTION_MENU_TEXT = "\u22EF";

    private final User currentUser;
    private final Runnable refreshCallback;
    private final Button button = new Button(ACTION_MENU_TEXT);

    public ActionButtonCell(User currentUser, Runnable refreshCallback) {
        this.currentUser = currentUser;
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
        Status status = row.status;
        ContextMenu menu = new ContextMenu();

        addItem(menu, "상세 보기",  () -> showIssueDetail(row));
        addItem(menu, "코멘트 추가", () -> addComment(row));

        if (currentUser != null && currentUser.isAdmin()) {
            addItem(menu, "이슈 삭제", () -> deleteIssue(row));
            if (status != null && status != Status.NEW)
                addItem(menu, "담당자 강제 변경", () -> changeAssignee(row));
            addItem(menu, "우선순위 변경", () -> changePriority(row));
        }

        if (currentUser != null && currentUser.isPL()) {
            if (status == Status.NEW)
                addItem(menu, "담당자 지정", () -> changeAssignee(row));
            addItem(menu, "우선순위 변경", () -> changePriority(row));
            if (status == Status.RESOLVED)
                addItem(menu, "이슈 닫기", () -> updateStatus(row, Status.CLOSED));
        }

        if (currentUser != null && currentUser.isDev()
                && status == Status.ASSIGNED && isCurrentUser(row.assignee))
            addItem(menu, "수정 완료", () -> updateStatus(row, Status.FIXED));

        if (currentUser != null && currentUser.isTester()
                && status == Status.FIXED && isCurrentUser(row.reporter)) {
            addItem(menu, "검증 통과", () -> updateStatus(row, Status.RESOLVED));
            addItem(menu, "재오픈",    () -> updateStatus(row, Status.REOPENED));
        }

        menu.show(button, Side.BOTTOM, 0, 0);
    }

    private void addItem(ContextMenu menu, String label, Runnable action) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> action.run());
        menu.getItems().add(item);
    }

    private void showIssueDetail(MainView.IssueRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Issue Detail");
        alert.setHeaderText(null);
        alert.setContentText(
                "Project: "  + row.projectName
                + "\nID: "       + row.issueId
                + "\nTitle: "    + row.title
                + "\nPriority: " + (row.priority != null ? row.priority.name() : "-")
                + "\nStatus: "   + (row.status   != null ? row.status.name()   : "-")
                + "\nReporter: " + row.reporter
                + "\nAssignee: " + row.assignee);
        alert.showAndWait();
    }

    private void addComment(MainView.IssueRow row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("코멘트 추가");
        dialog.setHeaderText(null);
        dialog.setContentText("Comment:");
        dialog.showAndWait().ifPresent(comment -> {
            if (!comment.trim().isEmpty())
                new Alert(Alert.AlertType.INFORMATION, "Comment added.", ButtonType.OK).showAndWait();
        });
    }

    private void deleteIssue(MainView.IssueRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this issue?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                getTableView().getItems().remove(row);
                if (refreshCallback != null) refreshCallback.run();
            }
        });
    }

    private void changeAssignee(MainView.IssueRow row) {
        TextInputDialog dialog = new TextInputDialog(row.assignee);
        dialog.setTitle("담당자 변경");
        dialog.setHeaderText(null);
        dialog.setContentText("Assignee:");
        dialog.showAndWait().ifPresent(assignee -> {
            if (!assignee.trim().isEmpty() && refreshCallback != null)
                refreshCallback.run();
        });
    }

    private void changePriority(MainView.IssueRow row) {
        ComboBox<Priority> combo = new ComboBox<>();
        combo.getItems().addAll(Priority.values());
        combo.setValue(row.priority != null ? row.priority : Priority.MAJOR);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("우선순위 변경");
        dialog.getDialogPane().setContent(combo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK && refreshCallback != null)
                refreshCallback.run();
        });
    }

    private void updateStatus(MainView.IssueRow row, Status status) {
        // TODO: IssueController의 해당 메서드 호출 (assignIssue, fixIssue, verifyIssue, closeIssue)
        if (refreshCallback != null) refreshCallback.run();
    }

    private boolean isCurrentUser(String loginId) {
        return currentUser != null && loginId != null
                && currentUser.getLoginId().equals(loginId);
    }
}