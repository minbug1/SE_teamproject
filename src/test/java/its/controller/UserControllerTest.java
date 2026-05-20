package its.controller;

import its.model.AccountStatus;
import its.model.Role;
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
                Role.ADMIN
        );
        userRepository.save(admin);

        normalUser = new User(
                userRepository.generateUserId(),
                "normal",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );
        userRepository.save(normalUser);
    }

    @Test
    void createUserByAdminShouldCreateUser() {
        User createdUser = userController.createUserByAdmin(
                admin,
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        assertNotNull(createdUser);
        assertEquals("dev1", createdUser.getLoginId());
        assertEquals(AccountStatus.ACTIVE, createdUser.getAccountStatus());
        assertEquals(Role.DEVELOPER, createdUser.getRole());

        User savedUser = userRepository.findByLoginId("dev1");
        assertNotNull(savedUser);
        assertEquals(createdUser.getUserId(), savedUser.getUserId());
    }

    @Test
    void createUserByAdminShouldFailWhenCurrentUserIsNotAdmin() {
        assertThrows(SecurityException.class, () -> {
            userController.createUserByAdmin(
                    normalUser,
                    "dev1",
                    "1234",
                    AccountStatus.ACTIVE,
                    Role.DEVELOPER
            );
        });
    }

    @Test
    void createUserByAdminShouldFailWhenRoleIsUnassigned() {
        assertThrows(IllegalArgumentException.class, () -> {
            userController.createUserByAdmin(
                    admin,
                    "user1",
                    "1234",
                    AccountStatus.ACTIVE,
                    Role.UNASSIGNED
            );
        });
    }

    @Test
    void approveUserShouldChangeStatusToActiveAndSetRole() {
        User pendingUser = new User(
                userRepository.generateUserId(),
                "pending",
                "1234"
        );
        userRepository.save(pendingUser);

        userController.approveUser(admin, pendingUser.getUserId(), Role.DEVELOPER);

        User approvedUser = userRepository.findByUserId(pendingUser.getUserId());

        assertNotNull(approvedUser);
        assertEquals(AccountStatus.ACTIVE, approvedUser.getAccountStatus());
        assertEquals(Role.DEVELOPER, approvedUser.getRole());
    }

    @Test
    void approveUserShouldFailWhenRoleIsUnassigned() {
        User pendingUser = new User(
                userRepository.generateUserId(),
                "pending",
                "1234"
        );
        userRepository.save(pendingUser);

        assertThrows(IllegalArgumentException.class, () -> {
            userController.approveUser(admin, pendingUser.getUserId(), Role.UNASSIGNED);
        });
    }

    @Test
    void rejectUserShouldChangeStatusToRejected() {
        User pendingUser = new User(
                userRepository.generateUserId(),
                "pending",
                "1234"
        );
        userRepository.save(pendingUser);

        userController.rejectUser(admin, pendingUser.getUserId());

        User rejectedUser = userRepository.findByUserId(pendingUser.getUserId());

        assertNotNull(rejectedUser);
        assertEquals(AccountStatus.REJECTED, rejectedUser.getAccountStatus());
    }

    @Test
    void deactivateUserShouldChangeStatusToDisabled() {
        userController.deactivateUser(admin, normalUser.getUserId());

        User disabledUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(disabledUser);
        assertEquals(AccountStatus.DISABLED, disabledUser.getAccountStatus());
    }

    @Test
    void changeRoleShouldChangeUserRole() {
        userController.changeRole(admin, normalUser.getUserId(), Role.DEVELOPER);

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertEquals(Role.DEVELOPER, changedUser.getRole());
    }

    @Test
    void changeAccountStatusShouldChangeUserStatus() {
        userController.changeAccountStatus(admin, normalUser.getUserId(), AccountStatus.DISABLED);

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertEquals(AccountStatus.DISABLED, changedUser.getAccountStatus());
    }

    @Test
    void deleteUserShouldRemoveUser() {
        long targetUserId = normalUser.getUserId();

        userController.deleteUser(admin, targetUserId);

        assertNull(userRepository.findByUserId(targetUserId));
    }

    @Test
    void changeLoginIdShouldChangeCurrentUsersLoginId() {
        userController.changeLoginId(normalUser, "newLoginId");

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertEquals("newLoginId", changedUser.getLoginId());
    }

    @Test
    void changeLoginIdShouldFailWhenDuplicated() {
        assertThrows(IllegalArgumentException.class, () -> {
            userController.changeLoginId(normalUser, "admin");
        });
    }

    @Test
    void changePasswordShouldChangeCurrentUsersPassword() {
        userController.changePassword(normalUser, "5678");

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertTrue(changedUser.matchesPassword("5678"));
    }

    @Test
    void findPendingUsersShouldReturnOnlyPendingUsers() {
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
    void findUsersByRoleShouldReturnMatchingUsers() {
        List<User> testers = userController.findUsersByRole(admin, Role.TESTER);

        assertEquals(1, testers.size());
        assertEquals("normal", testers.get(0).getLoginId());
    }

    @Test
    void resetPasswordByAdminShouldChangePasswordToTemporaryPassword() {
        userController.resetPasswordByAdmin(admin, normalUser.getUserId(), "0000");

        User changedUser = userRepository.findByUserId(normalUser.getUserId());

        assertNotNull(changedUser);
        assertTrue(changedUser.matchesPassword("0000"));
    }
}
