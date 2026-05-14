package its.repository;

import its.model.AccountStatus;
import its.model.Role;
import its.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based implementation of UserRepository.
 * Stores user data in a text file.
 *
 * File format:
 * userId,loginId,password,accountStatus,role
 *
 * Example:
 * 1,dev1,1234,ACTIVE,DEVELOPER
 * 2,tester1,1234,PENDING,UNASSIGNED
 *
 * @author hanung
 */

public class FileUserRepository implements UserRepository {

    private static final String DEFAULT_FILE_PATH = "data/users.txt";

    private File file;

    public FileUserRepository() {
        this(DEFAULT_FILE_PATH);
    }

    // Constructor for testing or custom file path.
    public FileUserRepository(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty.");
        }

        this.file = new File(filePath);
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize user storage file.", e);
        }
    }

    @Override
    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }

        List<User> users = findAll();

        for (User existingUser : users) {
            if (existingUser.getUserId() == user.getUserId()) {
                throw new IllegalArgumentException("User ID already exists.");
            }

            if (existingUser.getLoginId().equals(user.getLoginId())) {
                throw new IllegalArgumentException("Login ID already exists.");
            }
        }

        users.add(user);
        writeAll(users);
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
            if (user.getLoginId().equals(loginId)) {
                return user;
            }
        }

        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                User user = parseUser(line);
                users.add(user);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read user storage file.", e);
        }

        return users;
    }

    @Override
    public void deleteByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }

        List<User> users = findAll();
        List<User> remainingUsers = new ArrayList<>();

        for (User user : users) {
            if (user.getUserId() != userId) {
                remainingUsers.add(user);
            }
        }

        writeAll(remainingUsers);
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

    private User parseUser(String line) {
        String[] tokens = line.split(",");

        if (tokens.length != 5) {
            throw new IllegalArgumentException("Invalid user data format: " + line);
        }

        try {
            long userId = Long.parseLong(tokens[0]);
            String loginId = tokens[1];
            String password = tokens[2];
            AccountStatus accountStatus = AccountStatus.valueOf(tokens[3]);
            Role role = Role.valueOf(tokens[4]);

            return new User(userId, loginId, password, accountStatus, role);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + line, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid enum value in user data: " + line, e);
        }
    }

    private void writeAll(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (User user : users) {
                writer.write(toFileLine(user));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write user storage file.", e);
        }
    }

    private String toFileLine(User user) {
        return user.getUserId() + "," +
               user.getLoginId() + "," +
               user.getPassword() + "," +
               user.getAccountStatus().name() + "," +
               user.getRole().name();
    }
}