package its.view.swing;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.Issue;
import its.model.Project;
import its.model.User;

public class MainView extends JFrame {

    private JTable issueTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JComboBox<ProjectFilterItem> projectFilterComboBox;

    private IssueController issueController;
    private ProjectController projectController;
    private AuthController authController;
    private UserController userController;
    
    private User currentUser;
    private final List<Project> projects = new ArrayList<>();

    private static final int COL_PROJECT_ID = 0;
    private static final int COL_PROJECT = 1;
    private static final int COL_ID = 2;
    private static final int COL_NAME = 3;
    private static final int COL_PRIORITY = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_REPORTER = 6;
    private static final int COL_ASSIGNEE = 7;
    private static final int COL_ACTION = 8;

    private static final String[] COLUMNS = {
        "Project ID", "Project", "ID", "Issue Name", "Priority", "Status", "Reporter", "Assignee", ""
    };

    public MainView(AuthController authController,
                    IssueController issueController,
                    ProjectController projectController,
                    UserController userController,
                    User currentUser) {
        this.authController = authController;
        this.issueController = issueController;
        this.projectController = projectController;
        this.userController = userController;
        this.currentUser = currentUser;
        initUI();
    }

    private void initUI() {
        setTitle("Issue Tracker");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Issue Tracker");
        JButton logoutBtn = new JButton("Logout");
        JButton reportBtn = new JButton("+ Report Issue");

        logoutBtn.addActionListener(e -> onLogout());
        reportBtn.addActionListener(e -> onReportIssue());

        JPanel centerPanel = new JPanel();
        centerPanel.add(new JLabel("Project"));
        projectFilterComboBox = new JComboBox<>();
        projectFilterComboBox.addActionListener(e -> applyProjectFilter());
        centerPanel.add(projectFilterComboBox);

        JPanel rightPanel = new JPanel();
        rightPanel.add(reportBtn);
        rightPanel.add(logoutBtn);

        panel.add(title, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COL_ACTION;
            }
        };

        issueTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        issueTable.setRowSorter(tableSorter);
        issueTable.setRowHeight(36);
        issueTable.getTableHeader().setReorderingAllowed(false);

        issueTable.getColumn("").setCellRenderer(new ActionButtonRenderer());
        issueTable.getColumn("").setCellEditor(new ActionButtonEditor(currentUser));

        issueTable.getColumnModel().getColumn(COL_PROJECT_ID).setMinWidth(0);
        issueTable.getColumnModel().getColumn(COL_PROJECT_ID).setMaxWidth(0);
        issueTable.getColumnModel().getColumn(COL_PROJECT_ID).setPreferredWidth(0);
        issueTable.getColumnModel().getColumn(COL_PROJECT).setPreferredWidth(120);
        issueTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(40);
        issueTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
        issueTable.getColumnModel().getColumn(COL_PRIORITY).setPreferredWidth(80);
        issueTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_REPORTER).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_ASSIGNEE).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(48);
        issueTable.getColumnModel().getColumn(COL_ACTION).setMaxWidth(56);

        loadProjects();
        loadIssues();
        refreshProjectFilter();

        panel.add(new JScrollPane(issueTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadIssues() {
        tableModel.setRowCount(0);
        for (Project project : projects) {
            for (Issue issue : project.getIssues()) {
                tableModel.addRow(new Object[]{
                    project.getProjectId(),
                    project.getName(),
                    issue.getIssueId(),
                    issue.getTitle(),
                    issue.getPriority(),
                    issue.getStatus(),
                    getLoginIdOrDash(issue.getReporter()),
                    getLoginIdOrDash(issue.getAssignee()),
                    ""
                });
            }
        }
    }

    private void loadProjects() {
        projects.clear();
        projects.addAll(projectController.getAllProjects());

        // 전체 이슈 로드
        List<Issue> allIssues = issueController.getAllIssues();

        // projectId 기준으로 이슈를 프로젝트에 주입
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

    private String getLoginIdOrDash(User user) {
        return user == null ? "-" : user.getLoginId();
    }

    private void refreshProjectFilter() {
        projectFilterComboBox.removeAllItems();
        projectFilterComboBox.addItem(ProjectFilterItem.all());
        for (Project project : projects) {
            projectFilterComboBox.addItem(ProjectFilterItem.of(project));
        }
        projectFilterComboBox.setSelectedIndex(0);
    }

    private void applyProjectFilter() {
        if (tableSorter == null || projectFilterComboBox == null) {
            return;
        }

        ProjectFilterItem selectedItem = (ProjectFilterItem) projectFilterComboBox.getSelectedItem();
        if (selectedItem == null || selectedItem.isAll()) {
            tableSorter.setRowFilter(null);
            return;
        }

        int projectId = selectedItem.getProject().getProjectId();
        tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object id = entry.getValue(COL_PROJECT_ID);
                if (id instanceof Number) {
                    return ((Number) id).intValue() == projectId;
                }
                try {
                    return Integer.parseInt(id.toString()) == projectId;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
    }

    private void onReportIssue() {
        ProjectFilterItem selectedItem = (ProjectFilterItem) projectFilterComboBox.getSelectedItem();
        Project selectedProject = selectedItem == null ? null : selectedItem.getProject();
        if (selectedProject == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a project before reporting an issue.",
                    "Project Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ReportIssueView dialog = new ReportIssueView(
                this, issueController, projectController, currentUser, selectedProject);
        dialog.setVisible(true);
        refreshTable();
    }   

    private void onLogout() {
    int confirm = JOptionPane.showConfirmDialog(
        this, "Logout?", "Logout", JOptionPane.YES_NO_OPTION
    );
    if (confirm == JOptionPane.YES_OPTION) {
        dispose();
        new LoginView(authController, issueController,
                      userController, projectController).setVisible(true);
    }
}

    public void refreshTable() {
        loadProjects();
        loadIssues();
        refreshProjectFilter();
        applyProjectFilter();
    }

    public void open() {
        setVisible(true);
    }

    private static class ProjectFilterItem {
        private final Project project;

        private ProjectFilterItem(Project project) {
            this.project = project;
        }

        private static ProjectFilterItem all() {
            return new ProjectFilterItem(null);
        }

        private static ProjectFilterItem of(Project project) {
            return new ProjectFilterItem(project);
        }

        private boolean isAll() {
            return project == null;
        }

        private Project getProject() {
            return project;
        }

        @Override
        public String toString() {
            return isAll() ? "All Projects" : project.getName();
        }
    }
}
