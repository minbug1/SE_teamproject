package its.repository;

import its.model.AccountStatus;
import its.model.Role;
import its.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileUserRepositoryTest {

    @TempDir
    Path tempDir;

    private FileUserRepository userRepository;

    @BeforeEach
    void setUp() {
        String testFilePath = tempDir.resolve("users.json").toString();
        userRepository = new FileUserRepository(testFilePath);
    }

    @Test
    void constructorShouldCreateEmptyJsonFile() {
        List<User> users = userRepository.findAll();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void saveShouldStoreUser() {
        User user = new User(
                userRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(user);

        User foundUser = userRepository.findByLoginId("tester1");

        assertNotNull(foundUser);
        assertEquals(user.getUserId(), foundUser.getUserId());
        assertEquals("tester1", foundUser.getLoginId());
        assertEquals(AccountStatus.ACTIVE, foundUser.getAccountStatus());
        assertEquals(Role.TESTER, foundUser.getRole());
    }

    @Test
    void saveShouldFailWhenUserIdAlreadyExists() {
        User user1 = new User(
                1L,
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User user2 = new User(
                1L,
                "tester2",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user1);

        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void saveShouldFailWhenLoginIdAlreadyExists() {
        User user1 = new User(
                1L,
                "sameId",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User user2 = new User(
                2L,
                "sameId",
                "5678",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user1);

        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void updateShouldModifyExistingUser() {
        User user = new User(
                userRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.PENDING,
                Role.UNASSIGNED
        );

        userRepository.save(user);

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRole(Role.TESTER);
        user.setLoginId("testerChanged");

        userRepository.update(user);

        User updatedUser = userRepository.findByUserId(user.getUserId());

        assertNotNull(updatedUser);
        assertEquals("testerChanged", updatedUser.getLoginId());
        assertEquals(AccountStatus.ACTIVE, updatedUser.getAccountStatus());
        assertEquals(Role.TESTER, updatedUser.getRole());
    }

    @Test
    void updateShouldFailWhenUserDoesNotExist() {
        User user = new User(
                999L,
                "ghost",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.update(user);
        });
    }

    @Test
    void updateShouldFailWhenLoginIdDuplicatedWithOtherUser() {
        User user1 = new User(
                1L,
                "user1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User user2 = new User(
                2L,
                "user2",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user1);
        userRepository.save(user2);

        user2.setLoginId("user1");

        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.update(user2);
        });
    }

    @Test
    void deleteByUserIdShouldRemoveUser() {
        User user = new User(
                userRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(user);

        userRepository.deleteByUserId(user.getUserId());

        assertNull(userRepository.findByUserId(user.getUserId()));
        assertTrue(userRepository.findAll().isEmpty());
    }

    @Test
    void deleteByUserIdShouldFailWhenUserDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.deleteByUserId(999L);
        });
    }

    @Test
    void findByUserIdShouldReturnMatchingUser() {
        User user = new User(
                userRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(user);

        User foundUser = userRepository.findByUserId(user.getUserId());

        assertNotNull(foundUser);
        assertEquals("tester1", foundUser.getLoginId());
    }

    @Test
    void findByUserIdShouldReturnNullWhenInvalidId() {
        assertNull(userRepository.findByUserId(0));
        assertNull(userRepository.findByUserId(-1));
    }

    @Test
    void findByLoginIdShouldReturnMatchingUser() {
        User user = new User(
                userRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(user);

        User foundUser = userRepository.findByLoginId("tester1");

        assertNotNull(foundUser);
        assertEquals(user.getUserId(), foundUser.getUserId());
    }

    @Test
    void findByLoginIdShouldReturnNullWhenInvalidLoginId() {
        assertNull(userRepository.findByLoginId(null));
        assertNull(userRepository.findByLoginId(""));
        assertNull(userRepository.findByLoginId("   "));
    }

    @Test
    void findByAccountStatusShouldReturnMatchingUsers() {
        User pendingUser = new User(
                1L,
                "pending",
                "1234",
                AccountStatus.PENDING,
                Role.UNASSIGNED
        );

        User activeUser = new User(
                2L,
                "active",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(pendingUser);
        userRepository.save(activeUser);

        List<User> pendingUsers = userRepository.findByAccountStatus(AccountStatus.PENDING);

        assertEquals(1, pendingUsers.size());
        assertEquals("pending", pendingUsers.get(0).getLoginId());
    }

    @Test
    void findByRoleShouldReturnMatchingUsers() {
        User tester = new User(
                1L,
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User developer = new User(
                2L,
                "dev1",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(tester);
        userRepository.save(developer);

        List<User> developers = userRepository.findByRole(Role.DEVELOPER);

        assertEquals(1, developers.size());
        assertEquals("dev1", developers.get(0).getLoginId());
    }

    @Test
    void findAllShouldReturnAllUsers() {
        User user1 = new User(
                1L,
                "user1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User user2 = new User(
                2L,
                "user2",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    @Test
    void findPendingUsersShouldReturnOnlyPendingUsers() {
        User pendingUser = new User(
                1L,
                "pending",
                "1234"
        );

        User activeUser = new User(
                2L,
                "active",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        userRepository.save(pendingUser);
        userRepository.save(activeUser);

        List<User> pendingUsers = userRepository.findPendingUsers();

        assertEquals(1, pendingUsers.size());
        assertEquals("pending", pendingUsers.get(0).getLoginId());
        assertEquals(AccountStatus.PENDING, pendingUsers.get(0).getAccountStatus());
    }

    @Test
    void generateUserIdShouldReturnMaxIdPlusOne() {
        User user1 = new User(
                10L,
                "user10",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        User user2 = new User(
                20L,
                "user20",
                "1234",
                AccountStatus.ACTIVE,
                Role.DEVELOPER
        );

        userRepository.save(user1);
        userRepository.save(user2);

        long nextId = userRepository.generateUserId();

        assertEquals(21L, nextId);
    }

    @Test
    void dataShouldPersistAcrossRepositoryInstances() {
        String testFilePath = tempDir.resolve("persist-users.json").toString();

        FileUserRepository firstRepository = new FileUserRepository(testFilePath);

        User user = new User(
                firstRepository.generateUserId(),
                "tester1",
                "1234",
                AccountStatus.ACTIVE,
                Role.TESTER
        );

        firstRepository.save(user);

        FileUserRepository secondRepository = new FileUserRepository(testFilePath);

        User foundUser = secondRepository.findByLoginId("tester1");

        assertNotNull(foundUser);
        assertEquals(user.getUserId(), foundUser.getUserId());
        assertEquals("tester1", foundUser.getLoginId());
        assertEquals(AccountStatus.ACTIVE, foundUser.getAccountStatus());
        assertEquals(Role.TESTER, foundUser.getRole());
    }

    @Test
    void saveShouldFailWhenUserIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            userRepository.save(null);
        });
    }
}
