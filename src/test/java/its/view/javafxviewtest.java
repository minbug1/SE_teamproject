package its.view;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

// =========================================================================
// [기존] 1. JavaFX TableView용 데이터 모델 (DTO 역할)
// =========================================================================
class IssueTableViewModel {
    private final SimpleIntegerProperty issueId;
    private final SimpleStringProperty projectName;
    private final SimpleStringProperty title;
    private final SimpleStringProperty reporter;
    private final SimpleStringProperty assignee;

    public IssueTableViewModel(int issueId, String projectName, String title, String reporter, String assignee) {
        this.issueId = new SimpleIntegerProperty(issueId);
        this.projectName = new SimpleStringProperty(projectName);
        this.title = new SimpleStringProperty(title);
        this.reporter = new SimpleStringProperty(reporter);
        this.assignee = new SimpleStringProperty(assignee);
    }

    public int getIssueId() { return issueId.get(); }
    public SimpleIntegerProperty issueIdProperty() { return issueId; }
    public String getProjectName() { return projectName.get(); }
    public SimpleStringProperty projectNameProperty() { return projectName; }
    public String getTitle() { return title.get(); }
    public SimpleStringProperty titleProperty() { return title; }
    public String getReporter() { return reporter.get(); }
    public SimpleStringProperty reporterProperty() { return reporter; }
    public String getAssignee() { return assignee.get(); }
    public SimpleStringProperty assigneeProperty() { return assignee; }
}

// =========================================================================
// [추가] 2. AdminView용 사용자 관리 데이터 모델
// =========================================================================
class UserTableViewModel {
    private final SimpleStringProperty userId;
    private final SimpleStringProperty userName;
    private final SimpleStringProperty role;

    public UserTableViewModel(String userId, String userName, String role) {
        this.userId = new SimpleStringProperty(userId);
        this.userName = new SimpleStringProperty(userName);
        this.role = new SimpleStringProperty(role);
    }

    public String getUserId() { return userId.get(); }
    public SimpleStringProperty userIdProperty() { return userId; }
    public String getUserName() { return userName.get(); }
    public SimpleStringProperty userNameProperty() { return userName; }
    public String getRole() { return role.get(); }
    public SimpleStringProperty roleProperty() { return role; }
}

// =========================================================================
// 3. JavaFX MainView 클래스 구현
// =========================================================================
public class javafxviewtest extends Application {

    private final TableView<IssueTableViewModel> issueTable = new TableView<>();
    private final ObservableList<IssueTableViewModel> tableData = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        // 기존 메인 뷰 화면 구성 로직 (생략 가능)
    }

    public void loadProjectIssues(List<MockIssue> issues, String projectName) {
        tableData.clear();
        for (MockIssue issue : issues) {
            tableData.add(new IssueTableViewModel(
                    issue.getIssueId(), projectName, issue.getTitle(), issue.getReporterName(), issue.getAssigneeName()
            ));
        }
    }

    public TableView<IssueTableViewModel> getIssueTable() { return issueTable; }

    public static void main(String[] args) { launch(args); }
}

// =========================================================================
// [추가] 4. LoginView 클래스 구현 및 검증용 메서드
// =========================================================================
class LoginView {
    private String errorMessage = "";
    private boolean loginSuccess = false;

    // 로그인 인증 로직 시뮬레이션
    public void handleLogin(String id, String password) {
        if (id == null || id.isEmpty() || password == null || password.isEmpty()) {
            this.errorMessage = "아이디와 비밀번호를 모두 입력해주세요.";
            this.loginSuccess = false;
        } else if ("admin".equals(id) && "1234".equals(password)) {
            this.errorMessage = "";
            this.loginSuccess = true;
        } else {
            this.errorMessage = "계정 정보가 일치하지 않습니다.";
            this.loginSuccess = false;
        }
    }

    public String getErrorMessage() { return errorMessage; }
    public boolean isLoginSuccess() { return loginSuccess; }
}

// =========================================================================
// [추가] 5. AdminView 클래스 구현 (사용자 목록 TableView 포함)
// =========================================================================
class AdminView {
    private final TableView<UserTableViewModel> userTable = new TableView<>();
    private final ObservableList<UserTableViewModel> userData = FXCollections.observableArrayList();

    public AdminView() {
        // 테스트 시 컴포넌트 구조 유지를 위해 컬럼 바인딩
        TableColumn<UserTableViewModel, String> idCol = new TableColumn<>("사용자 ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        TableColumn<UserTableViewModel, String> nameCol = new TableColumn<>("이름");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        TableColumn<UserTableViewModel, String> roleCol = new TableColumn<>("권한");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        userTable.getColumns().addAll(idCol, nameCol, roleCol);
        userTable.setItems(userData);
    }

    // 관리자 기능: 가입된 전체 사용자 데이터 로드
    public void loadUserList(List<MockUser> users) {
        userData.clear();
        for (MockUser user : users) {
            userData.add(new UserTableViewModel(user.getUserId(), user.getUserName(), user.getRole()));
        }
    }

    public TableView<UserTableViewModel> getUserTable() { return userTable; }
}

// =========================================================================
// 6. 테스트용 단순화된 가상 모델들
// =========================================================================
class MockIssue {
    private final int issueId;
    private final String title;
    private final String reporterName;
    private final String assigneeName;

    public MockIssue(int issueId, String title, String reporterName, String assigneeName) {
        this.issueId = issueId; this.title = title; this.reporterName = reporterName; this.assigneeName = assigneeName;
    }
    public int getIssueId() { return issueId; }
    public String getTitle() { return title; }
    public String getReporterName() { return reporterName; }
    public String getAssigneeName() { return assigneeName; }
}

class MockUser {
    private final String userId;
    private final String userName;
    private final String role;

    public MockUser(String userId, String userName, String role) {
        this.userId = userId; this.userName = userName; this.role = role;
    }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getRole() { return role; }
}