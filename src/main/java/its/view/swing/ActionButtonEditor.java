package its.view.swing;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import its.model.Priority;
import its.model.Status;
import its.model.User;

public class ActionButtonEditor implements TableCellEditor {

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

    private JTable table;
    private Object currentValue;
    private int modelRow = -1;

    public ActionButtonEditor(User currentUser) {
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

        Status status = getStatus();
        if (currentUser != null && currentUser.isAdmin()) {
            addItem(menu, "이슈 삭제", this::deleteIssue);
            if (isAssignedOrLater(status)) {
                addItem(menu, "담당자 강제 변경", this::changeAssignee);
            }
            addItem(menu, "우선순위 변경", this::changePriority);
        }

        if (currentUser != null && currentUser.isPL()) {
            if (status == Status.NEW) {
                addItem(menu, "담당자 지정", this::changeAssignee);
            }
            addItem(menu, "우선순위 변경", this::changePriority);
            if (status == Status.RESOLVED) {
                addItem(menu, "이슈 닫기", () -> updateStatus(Status.CLOSED));
            }
        }

        if (currentUser != null && currentUser.isDev()
                && status == Status.ASSIGNED && isAssigneeCurrentUser()) {
            addItem(menu, "수정 완료", () -> updateStatus(Status.FIXED));
        }

        if (currentUser != null && currentUser.isTester()
                && status == Status.FIXED && isReporterCurrentUser()) {
            addItem(menu, "검증 통과", () -> updateStatus(Status.RESOLVED));
            addItem(menu, "재오픈", () -> updateStatus(Status.REOPENED));
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
        JOptionPane.showMessageDialog(button,
                "Project: " + getValue(COL_PROJECT)
                        + "\nID: " + getValue(COL_ID)
                        + "\nTitle: " + getValue(COL_NAME)
                        + "\nPriority: " + getValue(COL_PRIORITY)
                        + "\nStatus: " + getValue(COL_STATUS)
                        + "\nReporter: " + getValue(COL_REPORTER)
                        + "\nAssignee: " + getValue(COL_ASSIGNEE),
                "Issue Detail",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void addComment() {
        String comment = JOptionPane.showInputDialog(button, "Comment");
        if (comment != null && !comment.trim().isEmpty()) {
            JOptionPane.showMessageDialog(button, "Comment added.");
        }
    }

    private void deleteIssue() {
        if (JOptionPane.showConfirmDialog(button, "Delete this issue?", "Delete",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            stopCellEditing();
            ((javax.swing.table.DefaultTableModel) table.getModel()).removeRow(modelRow);
        }
    }

    private void changeAssignee() {
        String assignee = JOptionPane.showInputDialog(button, "Assignee", getValue(COL_ASSIGNEE));
        if (assignee != null && !assignee.trim().isEmpty()) {
            setValue(COL_ASSIGNEE, assignee.trim());
            if (getStatus() == Status.NEW || getStatus() == Status.REOPENED) {
                setValue(COL_STATUS, Status.ASSIGNED.name());
            }
        }
    }

    private void changePriority() {
        JComboBox<Priority> comboBox = new JComboBox<>(Priority.values());
        comboBox.setSelectedItem(getPriority());
        if (JOptionPane.showConfirmDialog(button, comboBox, "Priority",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            setValue(COL_PRIORITY, comboBox.getSelectedItem().toString());
        }
    }

    private void updateStatus(Status status) {
        setValue(COL_STATUS, status.name());
    }

    private boolean isAssignedOrLater(Status status) {
        return status != null && status != Status.NEW;
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

    private Status getStatus() {
        try {
            return Status.valueOf(String.valueOf(getValue(COL_STATUS)));
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

    private void fireEditingStopped() {
        ChangeEvent event = new ChangeEvent(this);
        for (CellEditorListener listener : listenerList.getListeners(CellEditorListener.class)) {
            listener.editingStopped(event);
        }
    }

    private void fireEditingCanceled() {
        ChangeEvent event = new ChangeEvent(this);
        for (CellEditorListener listener : listenerList.getListeners(CellEditorListener.class)) {
            listener.editingCanceled(event);
        }
    }
}
