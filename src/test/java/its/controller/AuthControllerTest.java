package its.controller;

import its.model.AccountStatus;
import its.model.UserRole;
import its.model.User;
import its.repository.MemoryUserRepository;
import its.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {

    private UserRepository userRepository;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        userRepository = new MemoryUserRepository();
        authController = new AuthController(userRepository);
    }

    @Test
    void register() {
        User user = authController.register("tester1", "1234");

        assertNotNull(user);
        assertEquals(1L, user.getUserId());
        assertEquals("tester1", user.getLoginId());
        assertEquals(AccountStatus.PENDING, user.getAccountStatus());
        assertEquals(UserRole.UNASSIGNED, user.getRole());

        User savedUser = userRepository.findByLoginId("tester1");
        assertNotNull(savedUser);
        assertEquals(user.getUserId(), savedUser.getUserId());
    }

    @Test
    void register_WhenLoginIdIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("   ", "1234");
        });
    }

    @Test
    void register_WhenPasswordIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("tester1", "   ");
        });
    }

    @Test
    void register_WhenLoginIdAlreadyExists() {
        authController.register("tester1", "1234");

        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("tester1", "5678");
        });
    }

    @Test
    void login() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                UserRole.DEVELOPER
        );

        userRepository.save(user);

        User loggedInUser = authController.login("dev1", "1234");

        assertNotNull(loggedInUser);
        assertEquals("dev1", loggedInUser.getLoginId());
        assertTrue(authController.isLoggedIn());
        assertEquals(loggedInUser, authController.getCurrentUser());
    }

    @Test
    void login_WhenLoginIdDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("unknown", "1234");
        });
    }

    @Test
    void login_WhenPasswordIsWrong() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                UserRole.DEVELOPER
        );

        userRepository.save(user);

        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("dev1", "wrong-password");
        });
    }

    @Test
    void login_WhenAccountIsPending() {
        User user = new User(
                userRepository.generateUserId(),
                "pendingUser",
                "1234",
                AccountStatus.PENDING,
                UserRole.UNASSIGNED
        );

        userRepository.save(user);

        assertThrows(IllegalStateException.class, () -> {
            authController.login("pendingUser", "1234");
        });
    }

    @Test
    void logout() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                UserRole.DEVELOPER
        );

        userRepository.save(user);

        authController.login("dev1", "1234");
        assertTrue(authController.isLoggedIn());

        authController.logout();

        assertFalse(authController.isLoggedIn());
        assertNull(authController.getCurrentUser());
    }
}
