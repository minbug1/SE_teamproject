package its.view;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.AccountStatus;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Project;
import its.model.User;
import its.model.UserRole;
import its.repository.IssueRepository;
import its.repository.ProjectRepository;
import its.repository.UserRepository;
import its.view.swing.MainView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewTest {

    private final List<JFrame> createdFrames = new ArrayList<>();

    @AfterEach
    void disposeFrames() throws Exception {
        runOnEdt(() -> {
            for (JFrame frame : createdFrames) {
                frame.dispose();
            }
            createdFrames.clear();
        });
    }

    @Test
    void mainViewShouldLoadProjectIssuesIntoTable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Fixture fixture = new Fixture(UserRole.ADMIN);
        runOnEdt(() -> {
            MainView view = fixture.createMainView();
            JTable table = findComponent(view, JTable.class);

            assertEquals(3, table.getModel().getRowCount());
            assertEquals("Project Alpha", table.getModel().getValueAt(0, 1));
            assertEquals("Login page fails", table.getModel().getValueAt(0, 3));
            assertEquals("tester", table.getModel().getValueAt(0, 6));
            assertEquals("dev", table.getModel().getValueAt(0, 7));
            assertEquals(0, table.getColumnModel().getColumn(0).getMaxWidth());
        });
    }

    @Test
    void mainViewShouldUseRoleBasedDefaultStatusFilter() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Fixture fixture = new Fixture(UserRole.TESTER);
        runOnEdt(() -> {
            MainView view = fixture.createMainView();
            JTable table = findComponent(view, JTable.class);
            JComboBox<?> statusComboBox = findComboBoxContaining(view, "FIXED");

            assertEquals("FIXED", String.valueOf(statusComboBox.getSelectedItem()));
            assertEquals(1, table.getRowCount());
            assertEquals("Login page fails", table.getValueAt(0, 3));
        });
    }

    @Test
    void mainViewShouldFilterMyIssuesByReporterOrAssignee() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Fixture fixture = new Fixture(UserRole.DEVELOPER);
        runOnEdt(() -> {
            MainView view = fixture.createMainView();
            JTable table = findComponent(view, JTable.class);
            JCheckBox myIssuesCheckBox = findCheckBox(view, "My Issues");

            assertEquals(2, table.getRowCount());

            myIssuesCheckBox.doClick();

            assertEquals(1, table.getRowCount());
            assertEquals("Assignment panel broken", table.getValueAt(0, 3));
            assertEquals("dev", table.getValueAt(0, 7));
        });
    }

    private static void runOnEdt(ThrowingRunnable runnable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
                throw (Exception) cause.getCause();
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private <T extends Component> T findComponent(Container container, Class<T> type) {
        if (type.isInstance(container)) {
            return type.cast(container);
        }
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T found = findComponent((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JComboBox<?> findComboBoxContaining(Container container, String itemText) {
        for (JComboBox<?> comboBox : findComponents(container, JComboBox.class)) {
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                if (itemText.equals(String.valueOf(comboBox.getItemAt(i)))) {
                    return comboBox;
                }
            }
        }
        throw new AssertionError("ComboBox item not found: " + itemText);
    }

    private JCheckBox findCheckBox(Container container, String text) {
        for (JCheckBox checkBox : findComponents(container, JCheckBox.class)) {
            if (text.equals(checkBox.getText())) {
                return checkBox;
            }
        }
        throw new AssertionError("CheckBox not found: " + text);
    }

    private <T extends Component> List<T> findComponents(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(container)) {
            result.add(type.cast(container));
        }
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container) {
                result.addAll(findComponents((Container) component, type));
            }
        }
        return result;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private class Fixture {
        private final MemoryIssueRepository issueRepository = new MemoryIssueRepository();
        private final MemoryProjectRepository projectRepository = new MemoryProjectRepository();
        private final MemoryUserRepository userRepository = new MemoryUserRepository();
        private final User currentUser;

        private Fixture(UserRole role) {
            User admin = user(1, "admin", UserRole.ADMIN);
            User pl = user(2, "pl", UserRole.PL);
            User dev = user(3, "dev", UserRole.DEVELOPER);
            User tester = user(4, "tester", UserRole.TESTER);
            User otherDev = user(5, "otherDev", UserRole.DEVELOPER);

            currentUser = switch (role) {
                case ADMIN -> admin;
                case PL -> pl;
                case DEVELOPER -> dev;
                case TESTER -> tester;
                default -> throw new IllegalArgumentException("Unsupported role for view fixture.");
            };

            List.of(admin, pl, dev, tester, otherDev).forEach(userRepository::save);

            Project project = new Project(1, "Project Alpha", "View test project");
            List.of(admin, pl, dev, tester, otherDev).forEach(project::addMember);

            Issue fixedIssue = issue(1, project, "Login page fails", tester, dev, IssueStatus.FIXED);
            Issue assignedIssue = issue(2, project, "Search result is slow", tester, otherDev, IssueStatus.ASSIGNED);
            Issue myAssignedIssue = issue(3, project, "Assignment panel broken", pl, dev, IssueStatus.ASSIGNED);

            issueRepository.save(fixedIssue);
            issueRepository.save(assignedIssue);
            issueRepository.save(myAssignedIssue);
            project.addIssue(fixedIssue);
            project.addIssue(assignedIssue);
            project.addIssue(myAssignedIssue);
            projectRepository.save(project);
        }

        private MainView createMainView() {
            MainView view = new MainView(
                    new AuthController(userRepository),
                    new IssueController(issueRepository),
                    new ProjectController(projectRepository),
                    new UserController(userRepository),
                    null,
                    null,
                    currentUser
            );
            createdFrames.add(view);
            return view;
        }

        private User user(long id, String loginId, UserRole role) {
            return new User(id, loginId, "pw", AccountStatus.ACTIVE, role);
        }

        private Issue issue(long id, Project project, String title, User reporter, User assignee, IssueStatus status) {
            Issue issue = new Issue(id, project.getProjectId(), title, "description", reporter, LocalDateTime.now());
            issue.setAssignee(assignee);
            issue.setStatus(status);
            return issue;
        }
    }

    private static class MemoryIssueRepository implements IssueRepository {
        private final List<Issue> issues = new ArrayList<>();

        @Override
        public void save(Issue issue) {
            issues.add(issue);
        }

        @Override
        public Issue findById(long issueId) {
            return issues.stream()
                    .filter(issue -> issue.getIssueId() == issueId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Issue findByProjectIdAndIssueId(int projectId, long issueId) {
            return issues.stream()
                    .filter(issue -> issue.getProjectId() == projectId && issue.getIssueId() == issueId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Issue> findAll() {
            return new ArrayList<>(issues);
        }

        @Override
        public List<Issue> findByProjectId(int projectId) {
            return issues.stream()
                    .filter(issue -> issue.getProjectId() == projectId)
                    .toList();
        }

        @Override
        public void update(Issue issue) {
            delete(issue.getProjectId(), issue.getIssueId());
            save(issue);
        }

        @Override
        public void delete(long issueId) {
            issues.removeIf(issue -> issue.getIssueId() == issueId);
        }

        @Override
        public void delete(int projectId, long issueId) {
            issues.removeIf(issue -> issue.getProjectId() == projectId && issue.getIssueId() == issueId);
        }

        @Override
        public long generateIssueId() {
            return issues.stream().mapToLong(Issue::getIssueId).max().orElse(0) + 1;
        }

        @Override
        public long generateIssueId(int projectId) {
            return issues.stream()
                    .filter(issue -> issue.getProjectId() == projectId)
                    .mapToLong(Issue::getIssueId)
                    .max()
                    .orElse(0) + 1;
        }
    }

    private static class MemoryProjectRepository implements ProjectRepository {
        private final List<Project> projects = new ArrayList<>();

        @Override
        public void save(Project project) {
            projects.add(project);
        }

        @Override
        public Project findById(int projectId) {
            return projects.stream()
                    .filter(project -> project.getProjectId() == projectId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects);
        }

        @Override
        public void update(Project project) {
            delete(project.getProjectId());
            save(project);
        }

        @Override
        public void delete(int projectId) {
            projects.removeIf(project -> project.getProjectId() == projectId);
        }

        @Override
        public int generateProjectId() {
            return projects.stream().mapToInt(Project::getProjectId).max().orElse(0) + 1;
        }
    }

    private static class MemoryUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();

        @Override
        public void save(User user) {
            users.add(user);
        }

        @Override
        public void update(User user) {
            deleteByUserId(user.getUserId());
            save(user);
        }

        @Override
        public void deleteByUserId(long userId) {
            users.removeIf(user -> user.getUserId() == userId);
        }

        @Override
        public User findByUserId(long userId) {
            return users.stream()
                    .filter(user -> user.getUserId() == userId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByLoginId(String loginId) {
            return users.stream()
                    .filter(user -> user.getLoginId().equals(loginId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<User> findByAccountStatus(AccountStatus accountStatus) {
            return users.stream()
                    .filter(user -> user.getAccountStatus() == accountStatus)
                    .toList();
        }

        @Override
        public List<User> findByRole(UserRole role) {
            return users.stream()
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }

        @Override
        public List<User> findPendingUsers() {
            return findByAccountStatus(AccountStatus.PENDING);
        }

        @Override
        public long generateUserId() {
            return users.stream().mapToLong(User::getUserId).max().orElse(0) + 1;
        }
    }
}
