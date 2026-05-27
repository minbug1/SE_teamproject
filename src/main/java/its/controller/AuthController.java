
package its.controller;


import its.model.AccountStatus;
import its.model.UserRole;
import its.model.User;
import its.repository.FileUserRepository;
import its.repository.UserRepository;

import java.util.List;

/*
 * controller for authentication (before login)
 * register, login, logout, getCurrentUser, isLoggedIn
 *
 * @author hanung
 */

public class AuthController {

    private final UserRepository userRepository;
    private User currentUser;

    public AuthController() {
        this(new FileUserRepository());
    }

    // test repository 사용 용도
    public AuthController(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null.");
        }

        this.userRepository = userRepository;
        this.currentUser = null;
    }

    // User register, approve는 User controller에
    public User register(String loginId, String password) {
        // 수정 가능
        validateLoginId(loginId);
        validatePassword(password);

        if (userRepository.findByLoginId(loginId) != null) {
            throw new IllegalArgumentException("Login ID already exists.");
        }

        long userId = userRepository.generateUserId();

        // User 생성자 따라감
        User newUser = new User(userId, loginId, password);
        userRepository.save(newUser);

        return newUser;
    }

    public User login(String loginId, String password) {
        validateLoginId(loginId);
        validatePassword(password);

        User user = userRepository.findByLoginId(loginId);

        

        if (user == null) {
            throw new IllegalArgumentException("Invalid Login ID or password.");
        }
        if (!user.matchesPassword(password)) {
            throw new IllegalArgumentException("Invalid Login ID or password.");
        }

        if (!user.isActive()) {
            if (user.isPending())  throw new IllegalStateException("Account is pending admin approval.");
            if (user.isRejected()) throw new IllegalStateException("Account has been rejected.");
            if (user.isDisabled()) throw new IllegalStateException("Account is disabled.");
            throw new IllegalStateException("Account is not active.");
       }

        currentUser = user;

        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public List<User> findPendingUsers(User adminUser) {
        validateAdmin(adminUser);
        return userRepository.findPendingUsers();
    }

    public List<User> findAllUsers(User adminUser) {
        validateAdmin(adminUser);
        return userRepository.findAll();
    }

    public void changeRole(User adminUser, long userId, UserRole newRole) {
        validateAdmin(adminUser);
        validateUserId(userId);

        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        User user = getExistingUser(userId);
        user.setRole(newRole);
        userRepository.update(user);
    }

    public void changeAccountStatus(User adminUser, long userId, AccountStatus newStatus) {
        validateAdmin(adminUser);
        validateUserId(userId);

        if (newStatus == null) {
            throw new IllegalArgumentException("Account status must not be null.");
        }

        User user = getExistingUser(userId);
        user.setAccountStatus(newStatus);
        userRepository.update(user);
    }

    public void approveUser(User adminUser, long userId, UserRole role) {
        validateAdmin(adminUser);
        validateUserId(userId);

        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        if (role == UserRole.UNASSIGNED) {
            throw new IllegalArgumentException("Approved user must have a role.");
        }

        User user = getExistingUser(userId);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.update(user);
    }

    public void rejectUser(User adminUser, long userId) {
        validateAdmin(adminUser);
        validateUserId(userId);

        User user = getExistingUser(userId);
        user.setAccountStatus(AccountStatus.REJECTED);
        userRepository.update(user);
    }

    public void deactivateUser(User adminUser, long userId) {
        validateAdmin(adminUser);
        validateUserId(userId);

        User user = getExistingUser(userId);
        user.setAccountStatus(AccountStatus.DISABLED);
        userRepository.update(user);
    }

    // helper
    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }
    }

    private void validateAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new IllegalArgumentException("Admin permission is required.");
        }
    }

    private User getExistingUser(long userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        return user;
    }
}
