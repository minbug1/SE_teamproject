package its.view.swing;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.model.AccountStatus;
import its.model.Project;
import its.model.Role;
import its.model.User;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AdminView extends JFrame {

    private static final String USERS_LABEL = "Users";

    private final AuthController authController;
    private final ProjectController projectController;
    private final User adminUser;
    private final List<Project> projects;
    private final List<User> allUsers;

    private DefaultListModel<String> projectListModel;
    private JList<String> projectList;

    private JLabel projectTitleLabel;
    private JLabel projectDescriptionLabel;
    private DefaultTableModel memberTableModel;
    private JTable memberTable;
    private DefaultTableModel unassignedTableModel;
    private JTable unassignedTable;

    private Project selectedProject;
    private boolean refreshingTables;

    public AdminView(AuthController authController,
                     ProjectController projectController,
                     IssueController issueController,
                     User adminUser,
                     List<Project> projects,
                     List<User> allUsers) {
        this.authController = authController;
        this.projectController = projectController;
        this.adminUser = adminUser;
        this.projects = projects;
        this.allUsers = allUsers;

        initUI();
    }

    private void initUI() {
        setTitle("Issue Tracker Admin");
        setSize(940, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createTopPanel(), BorderLayout.NORTH);
        add(createProjectListPanel(), BorderLayout.WEST);
        add(createMainPanel(), BorderLayout.CENTER);

        refreshProjectList();
        if (!projects.isEmpty()) {
            projectList.setSelectedIndex(0);
        } else {
            showUsers();
        }
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Issue Tracker Admin");
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> onLogout());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(new JLabel(adminUser.getLoginId()));
        rightPanel.add(logoutButton);

        panel.add(title, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    private JComponent createProjectListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Projects"));

        projectListModel = new DefaultListModel<>();
        projectList = new JList<>(projectListModel);
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onProjectSelectionChanged();
            }
        });

        JButton addProjectButton = new JButton("+ Project");
        addProjectButton.addActionListener(e -> doAddProject());

        panel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        panel.add(addProjectButton, BorderLayout.SOUTH);
        panel.setPreferredSize(new java.awt.Dimension(220, 0));
        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(createProjectHeaderPanel(), BorderLayout.NORTH);
        panel.add(createTablesPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProjectHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        projectTitleLabel = new JLabel("Project");
        projectDescriptionLabel = new JLabel(" ");
        titlePanel.add(projectTitleLabel);
        titlePanel.add(projectDescriptionLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton addMemberButton = new JButton("+ Member");
        editButton.addActionListener(e -> doEditProject());
        deleteButton.addActionListener(e -> doDeleteProject());
        addMemberButton.addActionListener(e -> doAddMember());
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(addMemberButton);

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createTablesPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        memberTableModel = new DefaultTableModel(
                new String[]{"Username", "Role", "Account Status", ""}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2 || column == 3;
            }
        };
        memberTable = createTable(memberTableModel);
        memberTable.getColumn("Role").setCellEditor(new EnumCellEditor<>(Role.class));
        memberTable.getColumn("Account Status").setCellEditor(new EnumCellEditor<>(AccountStatus.class));
        memberTable.getColumn("").setCellRenderer(new ButtonRenderer("Remove"));
        memberTable.getColumn("").setCellEditor(new RemoveMemberEditor());
        memberTableModel.addTableModelListener(e -> onMemberTableChanged(e));

        unassignedTableModel = new DefaultTableModel(
                new String[]{"Username", "Role", "Account Status", ""}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2 || column == 3;
            }
        };
        unassignedTable = createTable(unassignedTableModel);
        unassignedTable.getColumn("Role").setCellEditor(new EnumCellEditor<>(Role.class));
        unassignedTable.getColumn("Account Status").setCellEditor(new EnumCellEditor<>(AccountStatus.class));
        unassignedTable.getColumn("").setCellRenderer(new ButtonRenderer("Assign"));
        unassignedTable.getColumn("").setCellEditor(new AssignUserEditor());
        unassignedTableModel.addTableModelListener(e -> onUserTableChanged(e));

        JPanel memberPanel = new JPanel(new BorderLayout());
        memberPanel.setBorder(BorderFactory.createTitledBorder("Project Members"));
        memberPanel.add(new JScrollPane(memberTable), BorderLayout.CENTER);

        JPanel unassignedPanel = new JPanel(new BorderLayout());
        unassignedPanel.setBorder(BorderFactory.createTitledBorder(USERS_LABEL));
        unassignedPanel.add(new JScrollPane(unassignedTable), BorderLayout.CENTER);

        panel.add(memberPanel);
        panel.add(unassignedPanel);
        return panel;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private void onProjectSelectionChanged() {
        int index = projectList.getSelectedIndex();
        if (index < 0) {
            selectedProject = null;
            return;
        }

        if (index < projects.size()) {
            selectedProject = projects.get(index);
            refreshProjectPanel();
        } else {
            showUsers();
        }
    }

    private void refreshProjectList() {
        int previousIndex = projectList == null ? -1 : projectList.getSelectedIndex();

        projectListModel.clear();
        for (Project project : projects) {
            projectListModel.addElement(project.getName());
        }
        projectListModel.addElement(USERS_LABEL);

        if (previousIndex >= 0 && previousIndex < projectListModel.size()) {
            projectList.setSelectedIndex(previousIndex);
        }
    }

    private void refreshProjectPanel() {
        if (selectedProject == null) {
            return;
        }

        projectTitleLabel.setText(selectedProject.getName()
                + " (" + selectedProject.getMembers().size() + " members)");
        String description = selectedProject.getDescription();
        projectDescriptionLabel.setText(description == null || description.isBlank() ? " " : description);

        refreshingTables = true;
        memberTableModel.setRowCount(0);
        for (User user : selectedProject.getMembers()) {
            memberTableModel.addRow(new Object[]{
                    user.getLoginId(),
                    user.getRole(),
                    user.getAccountStatus(),
                    "Remove"
            });
        }

        refreshUserTable();
        refreshingTables = false;
    }

    private void showUsers() {
        selectedProject = null;
        projectTitleLabel.setText(USERS_LABEL);
        projectDescriptionLabel.setText("Pending users and project assignment.");
        refreshingTables = true;
        memberTableModel.setRowCount(0);
        refreshUserTable();
        refreshingTables = false;
    }

    private void refreshUserTable() {
        refreshingTables = true;
        unassignedTableModel.setRowCount(0);
        for (User user : getManagedUsers()) {
            unassignedTableModel.addRow(new Object[]{
                    user.getLoginId(),
                    user.getRole(),
                    user.getAccountStatus(),
                    "Assign"
            });
        }
        refreshingTables = false;
    }

    private List<User> getManagedUsers() {
        List<User> result = new ArrayList<>();
        for (User user : allUsers) {
            if (user.isAdmin()) {
                continue;
            }
            result.add(user);
        }
        return result;
    }

    private List<User> getUsersNotInSelectedProject() {
        List<User> result = new ArrayList<>();
        if (selectedProject == null) {
            return result;
        }

        for (User user : getManagedUsers()) {
            if (!selectedProject.getMembers().contains(user)) {
                result.add(user);
            }
        }
        return result;
    }

    private List<Project> getProjectsWithoutUser(User user) {
        List<Project> result = new ArrayList<>();
        for (Project project : projects) {
            if (!project.getMembers().contains(user)) {
                result.add(project);
            }
        }
        return result;
    }

    private void onMemberTableChanged(TableModelEvent event) {
        if (refreshingTables || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int row = event.getFirstRow();
        int column = event.getColumn();
        if (selectedProject == null || row < 0 || row >= selectedProject.getMembers().size()) {
            return;
        }
        if (column == 1 || column == 2) {
            updateUserFromTable(selectedProject.getMembers().get(row), memberTableModel, row, column);
        }
    }

    private void onUserTableChanged(TableModelEvent event) {
        if (refreshingTables || event.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int row = event.getFirstRow();
        int column = event.getColumn();
        List<User> users = getManagedUsers();
        if (row < 0 || row >= users.size()) {
            return;
        }
        if (column == 1 || column == 2) {
            updateUserFromTable(users.get(row), unassignedTableModel, row, column);
        }
    }

    private void updateUserFromTable(User user, DefaultTableModel model, int row, int column) {
            // 임시 디버그 — 어디서 호출되는지 추적
        System.out.println("=== updateUserFromTable 호출 ===");
        System.out.println("user: " + user.getLoginId());
        System.out.println("column: " + column);
        System.out.println("value: " + model.getValueAt(row, column));
        new Exception("호출 스택").printStackTrace();
        try {
            if (column == 1) {
                Role role = toRole(model.getValueAt(row, column));
                authController.changeRole(adminUser, user.getUserId(), role);
                user.setRole(role);
            } else if (column == 2) {
                AccountStatus status = toAccountStatus(model.getValueAt(row, column));
                authController.changeAccountStatus(adminUser, user.getUserId(), status);
                user.setAccountStatus(status);
            }
            refreshProjectList();
            if (selectedProject != null) {
                refreshProjectPanel();
            } else {
                showUsers();
            }
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        }
    }

    private Role toRole(Object value) {
        if (value instanceof Role) {
            return (Role) value;
        }
        return Role.valueOf(String.valueOf(value));
    }

    private AccountStatus toAccountStatus(Object value) {
        if (value instanceof AccountStatus) {
            return (AccountStatus) value;
        }
        return AccountStatus.valueOf(String.valueOf(value));
    }

    private void doAddProject() {
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();
        JPanel form = new JPanel(new GridLayout(4, 1, 4, 4));
        form.add(new JLabel("Project name"));
        form.add(nameField);
        form.add(new JLabel("Description"));
        form.add(descriptionField);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Add Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showWarning("Project name is required.");
            return;
        }
        for (Project project : projects) {
            if (project.getName().equals(name)) {
                showWarning("Project name already exists.");
                return;
            }
        }

        Project project = projectController.createProject(name, descriptionField.getText().trim(), adminUser);
        projects.add(project);
        refreshProjectList();
        projectList.setSelectedIndex(projects.size() - 1);
    }

    private void doEditProject() {
        if (selectedProject == null) {
            return;
        }

        JTextField nameField = new JTextField(selectedProject.getName());
        JTextField descriptionField = new JTextField(
                selectedProject.getDescription() == null ? "" : selectedProject.getDescription());
        JPanel form = new JPanel(new GridLayout(4, 1, 4, 4));
        form.add(new JLabel("Project name"));
        form.add(nameField);
        form.add(new JLabel("Description"));
        form.add(descriptionField);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Edit Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showWarning("Project name is required.");
            return;
        }

        selectedProject.setName(name);
        selectedProject.setDescription(descriptionField.getText().trim());
        projectController.updateProject(selectedProject, adminUser);
        refreshProjectList();
        refreshProjectPanel();
    }

    private void doDeleteProject() {
        if (selectedProject == null) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete '" + selectedProject.getName() + "'?",
                "Delete Project",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        projectController.deleteProject(selectedProject.getProjectId(), adminUser);
        projects.remove(selectedProject);
        selectedProject = null;
        refreshProjectList();
        if (!projects.isEmpty()) {
            projectList.setSelectedIndex(0);
        } else {
            showUsers();
        }
    }

    private void doAddMember() {
        if (selectedProject == null) {
            return;
        }

        List<User> candidates = getUsersNotInSelectedProject();
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available users.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] loginIds = candidates.stream().map(User::getLoginId).toArray(String[]::new);
        String selectedLoginId = (String) JOptionPane.showInputDialog(
                this,
                "Select user",
                "Add Member",
                JOptionPane.PLAIN_MESSAGE,
                null,
                loginIds,
                loginIds[0]);
        if (selectedLoginId == null) {
            return;
        }

        for (User user : candidates) {
            if (user.getLoginId().equals(selectedLoginId)) {
                projectController.addMemberToProject(selectedProject, user, adminUser);
                break;
            }
        }

        refreshProjectList();
        refreshProjectPanel();
    }

    private void doRemoveMember(int modelRow) {
        if (selectedProject == null || modelRow < 0 || modelRow >= selectedProject.getMembers().size()) {
            return;
        }

        User user = selectedProject.getMembers().get(modelRow);
        int result = JOptionPane.showConfirmDialog(
                this,
                "Remove '" + user.getLoginId() + "' from this project?",
                "Remove Member",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        projectController.removeMemberFromProject(selectedProject, user, adminUser);
        refreshProjectList();
        refreshProjectPanel();
    }

    private void doAssignToProject(int modelRow) {
        List<User> users = getManagedUsers();
        if (modelRow < 0 || modelRow >= users.size()) {
            return;
        }
        if (projects.isEmpty()) {
            showWarning("There are no projects.");
            return;
        }

        User user = users.get(modelRow);
        List<Project> candidates = getProjectsWithoutUser(user);
        if (candidates.isEmpty()) {
            showWarning("This user is already assigned to every project.");
            return;
        }

        String[] projectNames = candidates.stream().map(Project::getName).toArray(String[]::new);
        String selectedProjectName = (String) JOptionPane.showInputDialog(
                this,
                "Assign '" + user.getLoginId() + "' to project",
                "Assign User",
                JOptionPane.PLAIN_MESSAGE,
                null,
                projectNames,
                projectNames[0]);
        if (selectedProjectName == null) {
            return;
        }

        for (Project project : candidates) {
            if (project.getName().equals(selectedProjectName)) {
                projectController.addMemberToProject(project, user, adminUser);
                break;
            }
        }

        refreshProjectList();
        if (selectedProject != null) {
            refreshProjectPanel();
        } else {
            showUsers();
        }
    }

    private void onLogout() {
        int result = JOptionPane.showConfirmDialog(this, "Logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            new LoginView().setVisible(true);
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        private ButtonRenderer(String text) {
            setText(text);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    private class RemoveMemberEditor extends DefaultCellEditor {
        private final JButton button = new JButton("Remove");
        private int currentRow = -1;

        private RemoveMemberEditor() {
            super(new JCheckBox());
            button.addActionListener(e -> {
                fireEditingStopped();
                doRemoveMember(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = table.convertRowIndexToModel(row);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Remove";
        }
    }

    private class AssignUserEditor extends DefaultCellEditor {
        private final JButton button = new JButton("Assign");
        private int currentRow = -1;

        private AssignUserEditor() {
            super(new JCheckBox());
            button.addActionListener(e -> {
                fireEditingStopped();
                doAssignToProject(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = table.convertRowIndexToModel(row);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Assign";
        }
    }
    
    private class EnumCellEditor<E extends Enum<E>> extends DefaultCellEditor {

        private final JComboBox<E> comboBox;
        private boolean initializing = false;  // 초기화 중 플래그

        @SuppressWarnings("unchecked")
        EnumCellEditor(Class<E> enumClass) {
            super(new JComboBox<>(enumClass.getEnumConstants()));
            this.comboBox = (JComboBox<E>) editorComponent;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int col) {

            initializing = true;          // 초기화 시작
            comboBox.setSelectedItem(value);
            initializing = false;         // 초기화 완료

            return comboBox;
        }

        @Override
        public boolean stopCellEditing() {
            if (initializing) return false;  // 초기화 중이면 커밋 차단
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getSelectedItem();
        }
    }

    
}

