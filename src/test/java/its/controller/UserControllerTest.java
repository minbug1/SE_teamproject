package its.controller;

import its.model.AccountStatus;
import its.model.UserRole;
import its.model.User;
import its.repository.MemoryUserRepository;
import its.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserRepository userRepository;
    private UserController userController;

    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        userRepository = new MemoryUserRepository();
        userController = new UserController(userRepository);

        admin = new User(
                userRepository.generateUserId(),
                "admin",
                "1234",
                AccountStatus.ACTIVE,
                UserRole.ADMIN
        );
        userRepository.save(admin);

        normalUser = new User(
                userRepository.generateUserId(),
                "normal",
                "1234",
                AccountStatus.ACTIVE,
                UserRole.TESTER
        );
        userRepository.save(normalUser);
    }
    
    @Test
    void approveUser() {
        User pendingUser = new User(
                userRepository.generateUserId(),
                "pending",
                "1234"
        );
        userRepository.save(pendingUser);

        userController.approveUser(admin, pendingUser.getUserId(), UserRole.DEVELOPER);

        User approvedUser = userRepository.findByUserId(pendingUser.getUserId());

        assertNotNull(approvedUser);
        assertEquals(AccountStatus.ACTIVE, approvedUser.getAccountStatus());
        assertEquals(UserRole.DEVELOPER, approvedUser.getRole());
    }

    @Test
    void changeRole() {
        userController.changeRole(admin, normalUser.getUserId(), UserRole.DEVELOPER);

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertEquals(UserRole.DEVELOPER, changedUser.getRole());
    }

    @Test
    void changeAccountStatus() {
        userController.changeAccountStatus(admin, normalUser.getUserId(), AccountStatus.DISABLED);

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertEquals(AccountStatus.DISABLED, changedUser.getAccountStatus());
    }

    @Test
    void deleteUser() {
        long targetUserId = normalUser.getUserId();

        userController.deleteUser(admin, targetUserId);

        assertNull(userRepository.findByUserId(targetUserId));
    }

    @Test
    void findPendingUsers() {
        User pendingUser = new User(
                userRepository.generateUserId(),
                "pending",
                "1234"
        );
        userRepository.save(pendingUser);

        List<User> pendingUsers = userController.findPendingUsers(admin);

        assertEquals(1, pendingUsers.size());
        assertEquals("pending", pendingUsers.get(0).getLoginId());
        assertEquals(AccountStatus.PENDING, pendingUsers.get(0).getAccountStatus());
    }

    @Test
    void findUsersByRole() {
        List<User> testers = userController.findUsersByRole(admin, UserRole.TESTER);

        assertEquals(1, testers.size());
        assertEquals("normal", testers.get(0).getLoginId());
    }
}
