package its.view.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import its.controller.IssueController;
import its.controller.ProjectController;
import its.model.DeveloperRecommendation;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Project;
import its.model.User;

public class ActionButtonEditor implements TableCellEditor {

    private static final int COL_PROJECT_ID = 0;
    private static final int COL_PROJECT = 1;
    private static final int COL_ID = 2;
    private static final int COL_NAME = 3;
    private static final int COL_PRIORITY = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_REPORTER = 6;
    private static final int COL_ASSIGNEE = 7;
    private static final String ACTION_MENU_TEXT = "\u22EF";

    private final User currentUser;
    private final JButton button = new JButton(ACTION_MENU_TEXT);
    private final EventListenerList listenerList = new EventListenerList();
    
    private List<Project> projects;
    private IssueController issueController;
    private ProjectController projectController;

    private JTable table;
    private Object currentValue;
    private int modelRow = -1;

    public ActionButtonEditor(List<Project> projects, IssueController issueController, ProjectController projectController, User currentUser) {
        this.projects = projects;
        this.issueController = issueController;
        this.projectController = projectController;
        this.currentUser = currentUser;
        button.addActionListener(e -> showActionMenu());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        this.table = table;
        this.currentValue = value;
        this.modelRow = table.convertRowIndexToModel(row);
        button.setText(ACTION_MENU_TEXT);
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return currentValue;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    private void showActionMenu() {
        JPopupMenu menu = new JPopupMenu();
        addItem(menu, "상세 보기", this::showIssueDetail);
        addItem(menu, "코멘트 추가", this::addComment);

        IssueStatus status = getStatus();
        if (currentUser != null && currentUser.isAdmin()) {
            addItem(menu, "이슈 삭제", this::deleteIssue);
            if (isAssignedOrLater(status)) {
                addItem(menu, "담당자 강제 변경", this::changeAssignee);
            }
            addItem(menu, "우선순위 변경", this::changePriority);
        }

        if (currentUser != null && currentUser.isPL()) {
            if (status == IssueStatus.NEW) {
                addItem(menu, "담당자 지정", this::changeAssignee);
            }
            addItem(menu, "우선순위 변경", this::changePriority);
            if (status == IssueStatus.RESOLVED) {
                addItem(menu, "이슈 닫기", () -> closeIssue());
            }
        }

        if (currentUser != null && currentUser.isDev()
                && status == IssueStatus.ASSIGNED && isAssigneeCurrentUser()) {
            addItem(menu, "수정 완료", () -> fixIssue());
        }

        if (currentUser != null && currentUser.isTester()
                && status == IssueStatus.FIXED && isReporterCurrentUser()) {
            addItem(menu, "검증 통과", () -> verifyIssue(true));
            addItem(menu, "재오픈", () -> verifyIssue(false));
        }

        menu.show(button, 0, button.getHeight());
    }

    private void addItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            action.run();
            if (table != null && table.isEditing()) {
                stopCellEditing();
            }
        });
        menu.add(item);
    }

    private void showIssueDetail() {
        try {
            int projectId = ((Number) table.getModel().getValueAt(modelRow, COL_PROJECT_ID)).intValue();
            long issueId  = ((Number) getValue(COL_ID)).longValue();

            Project project = projectController.getAllProjects().stream()
                    .filter(p -> p.getProjectId() == projectId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Project not found."));

            Issue issue = issueController.getIssue(project, issueId);
            if (issue == null) {
                JOptionPane.showMessageDialog(button, "이슈를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new IssueDetailView(button, issue).setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(button,
                    "상세 보기 실패: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    
    }

    private void addComment() {
        JTextArea commentArea = new JTextArea(10, 30); // 행, 열 크기 증가
        commentArea.setFont(commentArea.getFont().deriveFont(12f));
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(commentArea);

        int result = JOptionPane.showConfirmDialog(
                button,
                scrollPane,
                "코멘트 입력",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String commentContent = commentArea.getText();
            if (commentContent == null || commentContent.trim().isEmpty()) return;

        try {
            int projectId = ((Number) table.getModel().getValueAt(modelRow, COL_PROJECT_ID)).intValue();
            long issueId  = ((Number) getValue(COL_ID)).longValue();

            // Project, 컨트롤러 통해 가져오기
            Project project = projectController.getAllProjects().stream()
                    .filter(p -> p.getProjectId() == projectId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Project not found."));

            issueController.addComment(project, issueId, commentContent, currentUser);

            JOptionPane.showMessageDialog(button, "Comment added.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(button,
                    "코멘트 추가 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
        }
    }

        // deleteIssue, changeAssignee, changePriority, updateStatus 메서드도 비슷한 패턴으로 구현
        private void deleteIssue() {
        if (JOptionPane.showConfirmDialog(button, "이슈를 삭제하시겠습니까?", "삭제",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
 
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        try {
            issueController.deleteIssue(project, issueId, currentUser);
            // 테이블 행 제거
            ((DefaultTableModel) table.getModel()).removeRow(modelRow);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }
    
    private void changeAssignee() {
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        List<User> devs = project.getMembers().stream()
                .filter(User::isDev)
                .collect(Collectors.toList());
 
        if (devs.isEmpty()) {
            JOptionPane.showMessageDialog(button,
                    "배정 가능한 개발자가 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
 
        // 현재 assignee 기본 선택
        User defaultDev = devs.stream()
                .filter(u -> u.getLoginId().equals(String.valueOf(getValue(COL_ASSIGNEE))))
                .findFirst()
                .orElse(devs.get(0));
 
        JComboBox<User> combo = new JComboBox<>(devs.toArray(new User[0]));
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getLoginId() : "");
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });
        combo.setSelectedItem(defaultDev);

        JTable recommendationTable = createRecommendationTable(project, issueId, combo);
        JPanel assignPanel = buildAssigneePanel(combo, recommendationTable);

        String comment = JOptionPane.showInputDialog(button, "코멘트 (선택):");
        if (comment == null) return; 
 
        if (JOptionPane.showConfirmDialog(button, assignPanel, "담당자 지정",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
 
        User selected = (User) combo.getSelectedItem();
        if (selected == null) return;
 
        try {
            issueController.assignIssue(project, issueId, selected, currentUser, comment);
            setValue(COL_ASSIGNEE, selected.getLoginId());
            setValue(COL_STATUS, IssueStatus.ASSIGNED.name());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private JPanel buildAssigneePanel(JComboBox<User> combo, JTable recommendationTable) {
        JPanel leftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 6, 0);
        leftPanel.add(new JLabel("담당자"), gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        leftPanel.add(combo, gbc);
        leftPanel.setPreferredSize(new Dimension(180, 220));

        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0;
        rightGbc.gridy = 0;
        rightGbc.anchor = GridBagConstraints.WEST;
        rightGbc.insets = new Insets(0, 0, 6, 0);
        rightPanel.add(new JLabel("담당자 추천"), rightGbc);

        rightGbc.gridy = 1;
        rightGbc.fill = GridBagConstraints.BOTH;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 1.0;
        rightPanel.add(new JScrollPane(recommendationTable), rightGbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.32);
        splitPane.setDividerLocation(180);
        splitPane.setPreferredSize(new Dimension(620, 260));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints panelGbc = new GridBagConstraints();
        panelGbc.gridx = 0;
        panelGbc.gridy = 0;
        panelGbc.fill = GridBagConstraints.BOTH;
        panelGbc.weightx = 1.0;
        panelGbc.weighty = 1.0;
        panel.add(splitPane, panelGbc);
        return panel;
    }

    private JTable createRecommendationTable(Project project, long issueId, JComboBox<User> assigneeCombo) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Developer", "Score", "Matched", "Solved"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try {
            List<DeveloperRecommendation> recommendations =
                    issueController.recommendAssignees(project, issueId, currentUser);

            for (DeveloperRecommendation recommendation : recommendations) {
                User developer = recommendation.getDeveloper();
                model.addRow(new Object[]{
                        developer != null ? developer.getLoginId() : "-",
                        String.format("%.3f", recommendation.getScore()),
                        recommendation.getMatchedIssueCount(),
                        recommendation.getTotalSolvedIssueCount()
                });
            }
        } catch (Exception ex) {
            model.addRow(new Object[]{"추천 실패", ex.getMessage(), "", ""});
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"추천 없음", "분류 후 사용 가능", "", ""});
        }

        JTable recommendationTable = new JTable(model);
        recommendationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recommendationTable.setPreferredScrollableViewportSize(new Dimension(390, 190));
        recommendationTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = recommendationTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            String loginId = String.valueOf(recommendationTable.getValueAt(row, 0));
            selectDeveloperByLoginId(assigneeCombo, loginId);
        });

        return recommendationTable;
    }

    private void selectDeveloperByLoginId(JComboBox<User> combo, String loginId) {
        if (combo == null || loginId == null || loginId.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < combo.getItemCount(); i++) {
            User user = combo.getItemAt(i);
            if (user != null && loginId.equals(user.getLoginId())) {
                combo.setSelectedItem(user);
                return;
            }
        }
    }

    private void changePriority() {
        JComboBox<Priority> combo = new JComboBox<>(Priority.values());
        combo.setSelectedItem(getPriority());
 
        if (JOptionPane.showConfirmDialog(button, combo, "우선순위 변경",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
 
        Priority selected = (Priority) combo.getSelectedItem();
        if (selected == null) return;
 
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        try {
            issueController.changePriority(project, issueId, selected, currentUser);
            setValue(COL_PRIORITY, selected.name());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // assigned -> fixed, assignee가 수정 완료 선택
    private void fixIssue() {
        String comment = JOptionPane.showInputDialog(button, "수정 내용 코멘트:");
        if (comment == null) return;
 
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        try {
            issueController.fixIssue(project, issueId, comment, currentUser);
            setValue(COL_STATUS, IssueStatus.FIXED.name());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // fixed -> resolved, reporter가 검증 통과/재오픈 선택
    private void verifyIssue(boolean isResolved) {
        String comment = JOptionPane.showInputDialog(button,
                isResolved ? "검증 통과 코멘트:" : "재오픈 사유:");
        if (comment == null) return;
 
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        try {
            issueController.verifyIssue(project, issueId, comment, currentUser, isResolved);
            setValue(COL_STATUS, isResolved ? IssueStatus.RESOLVED.name() : IssueStatus.REOPENED.name());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // resolved -> closed, PL이 이슈 닫기 선택
    private void closeIssue() {
        String comment = JOptionPane.showInputDialog(button, "종료 코멘트:");
        if (comment == null) return;
 
        Project project = getCurrentProject();
        long issueId    = getIssueId();
        if (project == null || issueId <= 0) return;
 
        try {
            issueController.closeIssue(project, issueId, comment, currentUser);
            setValue(COL_STATUS, IssueStatus.CLOSED.name());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // private void updateStatus(IssueStatus status) {
    //     setValue(COL_STATUS, status.name());
    // }

    private Project getCurrentProject() {
        Object projectIdVal = getValue(COL_PROJECT_ID);
        if (projectIdVal == null) return null;
        try {
            int projectId = Integer.parseInt(projectIdVal.toString());
            return projects.stream()
                    .filter(p -> p.getProjectId() == projectId)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
 
    private long getIssueId() {
        Object val = getValue(COL_ID);
        if (val == null) return -1;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void refreshRow(Project project, long issueId) {
        project.getIssues().stream()
                .filter(i -> i.getIssueId() == issueId)
                .findFirst()
                .ifPresent(issue -> {
                    setValue(COL_STATUS,   issue.getStatus().name());
                    setValue(COL_ASSIGNEE, issue.getAssignee() != null
                            ? issue.getAssignee().getLoginId() : "-");
                });
    }
 
    private boolean isAssignedOrLater(IssueStatus status) {
        return status != null && status != IssueStatus.NEW;
    }
 
    private boolean isAssigneeCurrentUser() {
        return isCurrentUser(getValue(COL_ASSIGNEE));
    }
 
    private boolean isReporterCurrentUser() {
        return isCurrentUser(getValue(COL_REPORTER));
    }
 
    private boolean isCurrentUser(Object value) {
        return currentUser != null && value != null
                && currentUser.getLoginId().equals(value.toString());
    }
 
    private IssueStatus getStatus() {
        try {
            return IssueStatus.valueOf(String.valueOf(getValue(COL_STATUS)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
 
    private Priority getPriority() {
        try {
            return Priority.valueOf(String.valueOf(getValue(COL_PRIORITY)));
        } catch (IllegalArgumentException e) {
            return Priority.MAJOR;
        }
    }
 
    private Object getValue(int column) {
        return table.getModel().getValueAt(modelRow, column);
    }
 
    private void setValue(int column, Object value) {
        TableModel model = table.getModel();
        model.setValueAt(value, modelRow, column);
    }
 
    private void showError(String message) {
        JOptionPane.showMessageDialog(button, message, "오류", JOptionPane.ERROR_MESSAGE);
    }
 
    private void fireEditingStopped() {
        ChangeEvent event = new ChangeEvent(this);
        for (CellEditorListener l : listenerList.getListeners(CellEditorListener.class)) {
            l.editingStopped(event);
        }
    }
 
    private void fireEditingCanceled() {
        ChangeEvent event = new ChangeEvent(this);
        for (CellEditorListener l : listenerList.getListeners(CellEditorListener.class)) {
            l.editingCanceled(event);
        }
    }
}
