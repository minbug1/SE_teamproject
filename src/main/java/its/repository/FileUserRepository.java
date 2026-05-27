package its.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import its.model.AccountStatus;
import its.model.UserRole;
import its.model.User;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Repository implementation for user
 * save, update, delete, find, generateUserId
 *
 * @author hanung
 */

public class FileUserRepository implements UserRepository {

    // file path for testing
    // private static final String FILE_PATH = "data/test_users.json";

    // actual file path
    private static final String FILE_PATH = "data/users.json";

    private final File file;
    private final Gson gson;

    public FileUserRepository() {
        this(FILE_PATH);
    }

    public FileUserRepository(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty.");
        }

        this.file = new File(filePath);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        initializeFile();
    }

    private void initializeFile() {
        try {
            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            if (file.length() == 0) {
                writeAll(new ArrayList<User>());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize user data file", e);
        }
    }

    @Override
    public void save(User user) {
        validateUser(user);

        List<User> users = findAll();

        for (User existingUser : users) {
            if (existingUser.getUserId() == user.getUserId()) {
                throw new IllegalArgumentException("Pre existing user with the same ID.");
            }

            if (Objects.equals(existingUser.getLoginId(), user.getLoginId())) {
                throw new IllegalArgumentException("Pre existing user with the same login ID.");
            }
        }

        users.add(user);
        writeAll(users);
    }

    @Override
    public void update(User user) {
        validateUser(user);

        List<User> users = findAll();
        boolean updated = false;

        for (int i = 0; i < users.size(); i++) {
            User existingUser = users.get(i);

            if (existingUser.getUserId() == user.getUserId()) {

                // login id duplication
                for (User otherUser : users) {
                    if (otherUser.getUserId() != user.getUserId()
                            && Objects.equals(otherUser.getLoginId(), user.getLoginId())) {
                        throw new IllegalArgumentException("Pre existing user with the same login ID.");
                    }
                }

                users.set(i, user);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("There is no user with the specified ID.");
        }

        writeAll(users);
    }

    @Override
    public void deleteByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID must be a positive number.");
        }

        List<User> users = findAll();
        List<User> remainingUsers = new ArrayList<>();
        boolean deleted = false;

        for (User user : users) {
            if (user.getUserId() == userId) {
                deleted = true;
            } else {
                remainingUsers.add(user);
            }
        }

        if (!deleted) {
            throw new IllegalArgumentException("There is no user with the specified ID.");
        }

        writeAll(remainingUsers);
    }

    @Override
    public User findByUserId(long userId) {
        if (userId <= 0) {
            return null;
        }

        List<User> users = findAll();

        for (User user : users) {
            if (user.getUserId() == userId) {
                return user;
            }
        }

        return null;
    }

    @Override
    public User findByLoginId(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            return null;
        }

        List<User> users = findAll();

        for (User user : users) {
            if (Objects.equals(user.getLoginId(), loginId)) {
                return user;
            }
        }

        return null;
    }

    @Override
    public List<User> findByAccountStatus(AccountStatus accountStatus) {
        if (accountStatus == null) {
            throw new IllegalArgumentException("Account status must not be null.");
        }

        List<User> result = new ArrayList<>();
        List<User> users = findAll();

        for (User user : users) {
            if (user.getAccountStatus() == accountStatus) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public List<User> findByRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        List<User> result = new ArrayList<>();
        List<User> users = findAll();

        for (User user : users) {
            if (user.getRole() == role) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public List<User> findAll() {
        List<UserRecord> records = readAllRecords();
        List<User> users = new ArrayList<>();

        for (UserRecord record : records) {
            users.add(record.toUser());
        }

        return users;
    }

    @Override
    public List<User> findPendingUsers() {
        return findByAccountStatus(AccountStatus.PENDING);
    }

    @Override
    public long generateUserId() {
        List<User> users = findAll();

        long maxId = 0;

        for (User user : users) {
            if (user.getUserId() > maxId) {
                maxId = user.getUserId();
            }
        }

        return maxId + 1;
    }

    private List<UserRecord> readAllRecords() {
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<UserRecord>>() {}.getType();
            List<UserRecord> records = gson.fromJson(reader, listType);

            if (records == null) {
                return new ArrayList<>();
            }

            return records;

        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Failed to parse user data file. The JSON format is invalid.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read user data file.", e);
        }
    }

    private void writeAll(List<User> users) {
        List<UserRecord> records = new ArrayList<>();

        for (User user : users) {
            records.add(UserRecord.fromUser(user));
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write user data file.", e);
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }

        if (user.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        if (user.getLoginId() == null || user.getLoginId().trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        if (user.getAccountStatus() == null) {
            throw new IllegalArgumentException("Account status must not be null.");
        }

        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
    }

    // DTO
    private static class UserRecord {
        private long userId;
        private String loginId;
        private String password;
        private String accountStatus;
        private String role;

        private User toUser() {
            if (userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID.");
            }

            if (loginId == null || loginId.trim().isEmpty()) {
                throw new IllegalArgumentException("Login ID must not be empty.");
            }

            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password must not be empty.");
            }

            if (accountStatus == null || accountStatus.trim().isEmpty()) {
                throw new IllegalArgumentException("Account status must not be empty.");
            }

            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Role must not be empty.");
            }

            try {
                return new User(
                        userId,
                        loginId,
                        password,
                        AccountStatus.valueOf(accountStatus),
                        UserRole.valueOf(role)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Saved account status or role value does not match the enum.", e);
            }
        }

        private static UserRecord fromUser(User user) {
            UserRecord record = new UserRecord();

            record.userId = user.getUserId();
            record.loginId = user.getLoginId();
            record.password = user.getPassword();
            record.accountStatus = user.getAccountStatus().name();
            record.role = user.getRole().name();

            return record;
        }
    }
}