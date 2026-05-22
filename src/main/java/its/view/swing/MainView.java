package its.view.swing;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import its.controller.IssueController;
import its.model.Project;
import its.model.User;

public class MainView extends JFrame {

    private JTable issueTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JComboBox<ProjectFilterItem> projectFilterComboBox;

    private IssueController issueController;
    private User currentUser;
    private final List<Project> projects = new ArrayList<>();

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_ACTION = 2;
    private static final int COL_PRIORITY = 3;
    private static final int COL_STATUS = 4;
    private static final int COL_REPORTER = 5;
    private static final int COL_ASSIGNEE = 6;

    private static final String[] COLUMNS = {
        "ID", "Issue Name", "Action",
        "Priority", "Status", "Reporter", "Assignee"
    };

    public MainView(IssueController controller, User currentUser) {
        this.issueController = controller;
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

        issueTable.getColumn("Action").setCellRenderer(new ActionButtonRenderer());
        issueTable.getColumn("Action").setCellEditor(new ActionButtonEditor(currentUser));

        issueTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(40);
        issueTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
        issueTable.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(100);
        issueTable.getColumnModel().getColumn(COL_PRIORITY).setPreferredWidth(80);
        issueTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_REPORTER).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_ASSIGNEE).setPreferredWidth(90);

        loadDummyData();
        loadDummyProjects();
        refreshProjectFilter();

        panel.add(new JScrollPane(issueTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadDummyData() {
        tableModel.addRow(new Object[]{
            1, "Login button click error", "Action", "CRITICAL", "NEW", "tester1", "-"
        });
        tableModel.addRow(new Object[]{
            2, "Duplicate email check missing", "Action", "MAJOR", "ASSIGNED", "tester2", "dev1"
        });
        tableModel.addRow(new Object[]{
            3, "Profile image upload failed", "Action", "MINOR", "FIXED", "tester1", "dev3"
        });
    }

    private void loadDummyProjects() {
        Project authProject = new Project(1, "Auth", "Login and account issues");
        authProject.getIssueIds().add(1);
        authProject.getIssueIds().add(2);

        Project profileProject = new Project(2, "Profile", "User profile issues");
        profileProject.getIssueIds().add(3);

        projects.clear();
        projects.add(authProject);
        projects.add(profileProject);
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

        Set<Integer> issueIds = new HashSet<>(selectedItem.getProject().getIssueIds());
        tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object id = entry.getValue(COL_ID);
                if (id instanceof Number) {
                    return issueIds.contains(((Number) id).intValue());
                }
                try {
                    return issueIds.contains(Integer.parseInt(id.toString()));
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
    }

    private void onReportIssue() {
        ReportIssueView dialog = new ReportIssueView(this, issueController, currentUser);
        dialog.setVisible(true);
    }

    private void onLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Logout?", "Logout", JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginView().setVisible(true);
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        loadDummyData();
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
