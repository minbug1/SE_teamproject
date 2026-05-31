package its.view.javafx;

import its.controller.IssueController;
import its.model.Priority;
import its.model.IssueStatus;
import its.model.User;
import javafx.geometry.Side;
import javafx.scene.control.*;

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
<<<<<<< HEAD
            if (status != null && status != Status.NEW)
                addItem(menu, "담당자 강제 변경", () -> changeAssignee(row, true));
=======
            if (status != null && status != IssueStatus.NEW)
                addItem(menu, "담당자 강제 변경", () -> changeAssignee(row));
>>>>>>> 3b7cae910723e0a4b724f840ca524f41e266796a
            addItem(menu, "우선순위 변경", () -> changePriority(row));
        }

        if (currentUser != null && currentUser.isPL()) {
<<<<<<< HEAD
            if (status == Status.NEW || status == Status.REOPENED)
                addItem(menu, "담당자 지정", () -> changeAssignee(row, false));
=======
            if (status == IssueStatus.NEW)
                addItem(menu, "담당자 지정", () -> changeAssignee(row));
>>>>>>> 3b7cae910723e0a4b724f840ca524f41e266796a
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
        TextArea area = buildTextArea("코멘트를 입력하세요.");
        showDialogWithContent("코멘트 추가", area, () -> {
            String content = area.getText().trim();
            if (content.isEmpty()) return;
            try {
                issueController.addComment(row.project, (int) row.issueId, content, currentUser);
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
                issueController.deleteIssue(row.project, (int) row.issueId, currentUser);
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 담당자 지정/변경 ──────────────────────────
    private void changeAssignee(MainView.IssueRow row, boolean isAdmin) {
        List<User> candidates = isAdmin
                ? row.project.getMembers()
                : row.project.getMembers().stream().filter(User::isDev).collect(Collectors.toList());

        if (candidates.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "배정 가능한 멤버가 없습니다.").showAndWait();
            return;
        }

        List<String> loginIds = candidates.stream().map(User::getLoginId).collect(Collectors.toList());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(loginIds.get(0), loginIds);
        dialog.setTitle(isAdmin ? "담당자 강제 변경" : "담당자 지정");
        dialog.setHeaderText(null);
        dialog.setContentText("담당자 선택:");

        dialog.showAndWait().ifPresent(selected -> {
            User assignee = candidates.stream()
                    .filter(u -> u.getLoginId().equals(selected))
                    .findFirst().orElse(null);
            if (assignee == null) return;
            try {
                if (isAdmin) {
                    issueController.forceAssignIssue(row.project, (int) row.issueId, assignee, currentUser, null);
                } else {
                    issueController.assignIssue(row.project, (int) row.issueId, assignee, currentUser, null);
                }
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 우선순위 변경 ─────────────────────────────
    private void changePriority(MainView.IssueRow row) {
        ComboBox<Priority> combo = new ComboBox<>();
        combo.getItems().addAll(Priority.values());
        combo.setValue(row.priority != null ? row.priority : Priority.MAJOR);

        showDialogWithContent("우선순위 변경", combo, () -> {
            try {
                issueController.changePriority(row.project, (int) row.issueId, combo.getValue(), currentUser);
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

<<<<<<< HEAD
    // ── 상태 변경 (수정완료 / 검증 / 재오픈 / 닫기) ──
    private void updateStatus(MainView.IssueRow row, Status targetStatus) {
        TextArea area = buildTextArea("코멘트 입력 (선택)");

        showDialogWithContent(getStatusLabel(targetStatus), area, () -> {
            String comment = area.getText().trim().isEmpty() ? null : area.getText().trim();
            try {
                switch (targetStatus) {
                    case FIXED:
                        issueController.fixIssue(row.project, (int) row.issueId, comment, currentUser);
                        break;
                    case RESOLVED:
                        issueController.verifyIssue(row.project, (int) row.issueId, comment, currentUser, true);
                        break;
                    case REOPENED:
                        issueController.verifyIssue(row.project, (int) row.issueId, comment, currentUser, false);
                        break;
                    case CLOSED:
                        issueController.closeIssue(row.project, (int) row.issueId, comment, currentUser);
                        break;
                    default:
                        break;
                }
                refresh();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
    }

    // ── 공통 유틸 ─────────────────────────────────
    private void showDialogWithContent(String title, javafx.scene.Node content, Runnable onOk) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> { if (bt == ButtonType.OK) onOk.run(); });
    }

    private TextArea buildTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.setPrefWidth(300);
        area.setPromptText(prompt);
        return area;
    }

    private String getStatusLabel(Status status) {
        switch (status) {
            case FIXED:    return "수정 완료";
            case RESOLVED: return "검증 통과";
            case REOPENED: return "재오픈";
            case CLOSED:   return "이슈 닫기";
            default:       return "상태 변경";
        }
=======
    private void updateStatus(MainView.IssueRow row, IssueStatus status) {
        // TODO: IssueController의 해당 메서드 호출 (assignIssue, fixIssue, verifyIssue, closeIssue)
        if (refreshCallback != null) refreshCallback.run();
>>>>>>> 3b7cae910723e0a4b724f840ca524f41e266796a
    }

    private boolean isCurrentUser(String loginId) {
        return currentUser != null && loginId != null
                && currentUser.getLoginId().equals(loginId);
    }
<<<<<<< HEAD

    private void refresh() {
        if (refreshCallback != null) refreshCallback.run();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
=======
}
>>>>>>> 3b7cae910723e0a4b724f840ca524f41e266796a
