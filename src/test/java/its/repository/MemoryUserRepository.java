package its.repository;

import its.model.AccountStatus;
import its.model.UserRole;
import its.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Test-only in-memory implementation
 * Override FileUserRepository
 * 
 * @author hanung
 */
public class MemoryUserRepository implements UserRepository {

    // memory
    private final List<User> users = new ArrayList<>();

    @Override
    public void save(User user) {
        validateUser(user);

        for (User existingUser : users) {
            if (existingUser.getUserId() == user.getUserId()) {
                throw new IllegalArgumentException("User ID already exists.");
            }

            if (Objects.equals(existingUser.getLoginId(), user.getLoginId())) {
                throw new IllegalArgumentException("Login ID already exists.");
            }
        }

        users.add(user);
    }

    @Override
    public void update(User user) {
        validateUser(user);

        boolean updated = false;

        for (int i = 0; i < users.size(); i++) {
            User existingUser = users.get(i);

            if (existingUser.getUserId() == user.getUserId()) {

                for (User otherUser : users) {
                    if (otherUser.getUserId() != user.getUserId()
                            && Objects.equals(otherUser.getLoginId(), user.getLoginId())) {
                        throw new IllegalArgumentException("Login ID already exists.");
                    }
                }

                users.set(i, user);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("User does not exist.");
        }
    }

    @Override
    public void deleteByUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }

        boolean deleted = false;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId() == userId) {
                users.remove(i);
                deleted = true;
                break;
            }
        }

        if (!deleted) {
            throw new IllegalArgumentException("User does not exist.");
        }
    }

    @Override
    public User findByUserId(long userId) {
        if (userId <= 0) {
            return null;
        }

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

        for (User user : users) {
            if (user.getAccountStatus() == accountStatus) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public List<User> findByRole(UserRole UserRole) {
        if (UserRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        List<User> result = new ArrayList<>();

        for (User user : users) {
            if (user.getRole() == UserRole) {
                result.add(user);
            }
        }

        return result;
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
        long maxId = 0;

        for (User user : users) {
            if (user.getUserId() > maxId) {
                maxId = user.getUserId();
            }
        }

        return maxId + 1;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }

        if (user.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
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
}
