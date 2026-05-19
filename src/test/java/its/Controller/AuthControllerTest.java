package its.controller;

import its.model.AccountStatus;
import its.model.Role;
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
    void registerShouldCreatePendingUnassignedUser() {
        User user = authController.register("tester1", "1234");

        assertNotNull(user);
        assertEquals(1L, user.getUserId());
        assertEquals("tester1", user.getLoginId());
        assertEquals(AccountStatus.PENDING, user.getAccountStatus());
        assertEquals(Role.UNASSIGNED, user.getRole());

        User savedUser = userRepository.findByLoginId("tester1");
        assertNotNull(savedUser);
        assertEquals(user.getUserId(), savedUser.getUserId());
    }

    @Test
    void registerShouldFailWhenLoginIdIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("   ", "1234");
        });
    }

    @Test
    void registerShouldFailWhenPasswordIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("tester1", "   ");
        });
    }

    @Test
    void registerShouldFailWhenLoginIdAlreadyExists() {
        authController.register("tester1", "1234");

        assertThrows(IllegalArgumentException.class, () -> {
            authController.register("tester1", "5678");
        });
    }

    @Test
    void loginShouldSucceedWhenUserIsActiveAndPasswordMatches() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user);

        User loggedInUser = authController.login("dev1", "1234");

        assertNotNull(loggedInUser);
        assertEquals("dev1", loggedInUser.getLoginId());
        assertTrue(authController.isLoggedIn());
        assertEquals(loggedInUser, authController.getCurrentUser());
    }

    @Test
    void loginShouldFailWhenLoginIdDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("unknown", "1234");
        });
    }

    @Test
    void loginShouldFailWhenPasswordIsWrong() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user);

        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("dev1", "wrong-password");
        });
    }

    @Test
    void loginShouldFailWhenAccountIsPending() {
        User user = new User(
                userRepository.generateUserId(),
                "pendingUser",
                "1234",
                AccountStatus.PENDING,
                Role.UNASSIGNED
        );

        userRepository.save(user);

        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("pendingUser", "1234");
        });
    }

    @Test
    void loginShouldFailWhenAccountIsDisabled() {
        User user = new User(
                userRepository.generateUserId(),
                "disabledUser",
                "1234",
                AccountStatus.DISABLED,
                Role.DEVELOPER
        );

        userRepository.save(user);

        assertThrows(IllegalArgumentException.class, () -> {
            authController.login("disabledUser", "1234");
        });
    }

    @Test
    void logoutShouldClearCurrentUser() {
        User user = new User(
                userRepository.generateUserId(),
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user);

        authController.login("dev1", "1234");
        assertTrue(authController.isLoggedIn());

        authController.logout();

        assertFalse(authController.isLoggedIn());
        assertNull(authController.getCurrentUser());
    }
}