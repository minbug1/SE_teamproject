package its.view;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class MainViewJavaFXTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // 이미 초기화된 경우 안전하게 처리
        }
    }


    // LoginView 테스트 (성공 및 실패 케이스 검증)
    @Test
    public void loginViewShouldValidateCredentialsCorrectly() throws InterruptedException {
        LoginView loginView = new LoginView();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Case 1: 올바른 계정 정보 입력 시 성공해야 함
                loginView.handleLogin("admin", "1234");
                assertTrue(loginView.isLoginSuccess(), "올바른 계정이면 로그인에 성공해야 합니다.");
                assertEquals("", loginView.getErrorMessage());

                // Case 2: 잘못된 비밀번호 입력 시 실패 및 에러 메시지 검증
                loginView.handleLogin("admin", "wrong_pwd");
                assertFalse(loginView.isLoginSuccess(), "비밀번호가 다르면 로그인에 실패해야 합니다.");
                assertEquals("계정 정보가 일치하지 않습니다.", loginView.getErrorMessage());

                // Case 3: 빈 값 입력 시 유효성 검증 메시지 확인
                loginView.handleLogin("", "");
                assertFalse(loginView.isLoginSuccess());
                assertEquals("아이디와 비밀번호를 모두 입력해주세요.", loginView.getErrorMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    //  AdminView 테스트 (사용자 관리 TableView 검증)
    @Test
    public void adminViewShouldLoadUserListIntoTable() throws InterruptedException {
        AdminView adminView = new AdminView();
        
        List<MockUser> mockUsers = new ArrayList<>();
        mockUsers.add(new MockUser("user01", "홍길동", "DEVELOPER"));
        mockUsers.add(new MockUser("user02", "김철수", "TESTER"));

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // When: 관리자 화면에 유저 목록 적재
                adminView.loadUserList(mockUsers);

                // Then: 테이블 데이터 개수 및 매핑 검증
                TableView<UserTableViewModel> table = adminView.getUserTable();
                assertEquals(2, table.getItems().size(), "유저 테이블의 행 개수는 2개여야 합니다.");

                UserTableViewModel firstRow = table.getItems().get(0);
                assertEquals("user01", firstRow.getUserId());
                assertEquals("홍길동", firstRow.getUserName());
                assertEquals("DEVELOPER", firstRow.getRole());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}