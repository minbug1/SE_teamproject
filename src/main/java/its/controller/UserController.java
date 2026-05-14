package its.controller;

import its.model.AccountStatus;
import its.model.Role;
import its.model.User;
import its.repository.FileUserRepository;
import its.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for user account management.
 * Handles creating, finding, listing, deleting users, and changing passwords.
 *
 * @author hanung
 */

public class UserController {

    private final UserRepository userRepository;

    public UserController() {
        this(new FileUserRepository());
    }

    // Constructor for testing or repository replacement.
    public UserController(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null.");
        }

        this.userRepository = userRepository;
    }

    // user register 없이 admin이 직접 생성, Q : accountStatus Active default
    public User createUserByAdmin(User currentUser, String loginId, String password, AccountStatus accountStatus, Role role) {
        if (currentUser == null || !currentUser.isAdmin()) {
        throw new IllegalArgumentException("Only admin can create users.");
    }

    if (loginId == null || loginId.trim().isEmpty()) {
        throw new IllegalArgumentException("Login ID must not be empty.");
    }

    if (password == null || password.trim().isEmpty()) {
        throw new IllegalArgumentException("Password must not be empty.");
    }

    if (accountStatus == null) {
        throw new IllegalArgumentException("AccountStatus must not be null.");
    }

    if (role == null) {
        throw new IllegalArgumentException("Role must not be null.");
    }

    if (role == Role.UNASSIGNED) {
        throw new IllegalArgumentException("Admin-created user must have a role.");
    }

    if (userRepository.findByLoginId(loginId) != null) {
        throw new IllegalArgumentException("Login ID already exists.");
    }

    long userId = userRepository.generateUserId();

    User user = new User(
            userId,
            loginId,
            password,
            accountStatus,
            role
    );

    userRepository.save(user);

    return user;
    }

    public void deleteUser(User currentUser, long userId) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalArgumentException("Only admin can delete users.");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be empty.");
        }

        User user = userRepository.findById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User does not exist.");
        }

        userRepository.deleteById(userId);
    }

    // find
    public User findUserByUserId(long userId) {
        validateUserId(userId);

        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        return user;
    }

    public User findUserByLoginId(String loginId) {
        validateLoginId(loginId);

        User user = userRepository.findByLoginId(loginId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        return user;
    }

    public List<User> findUsersByAccountStatus(AccountStatus accountStatus) {
        if (accountStatus == null) {
            throw new IllegalArgumentException("Account Status must not be null");
        }

        return userRepository.findByAccountStatus(accountStatus);
    }

    public List<User> findUsersByRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        return userRepository.findByRole(role);
    }

    public List<User> findAllUsers(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Login is required.");
        }

        if (!currentUser.isAdmin() && !currentUser.isPL()) {
            throw new IllegalArgumentException("Only admin or PL can view users.");
        }

        return userRepository.findAll();
    }
    
    // change
    public void changeLoginId(long userId, String newLoginId) {
        validateUserId(userId);
        validateLoginId(newLoginId);

        User duplicatedUser = userRepository.findByLoginId(newLoginId);

        if (duplicatedUser != null && !duplicatedUser.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Login ID already exists.");
        }

        User user = findUserByUserId(userId);
        user.changeLoginId(newLoginId);

        userRepository.update(user);
    }

    public void changePassword(long userId, String newPassword) {
        validateUserId(userId);

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        User user = findUserByUserId(userId);
        user.changePassword(newPassword);

        userRepository.update(user);
    }

     public void changeRole(long userId, Role newRole) {
        validateUserId(userId);

        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        User user = findUserByUserId(userId);
        user.changeRole(newRole);

        userRepository.update(user);
    }

    public void changeAccountStatus(long userId, AccountStatus newStatus) {
        validateUserId(userId);

        if (newStatus == null) {
            throw new IllegalArgumentException("Account status must not be null.");
        }

        User user = findUserByUserId(userId);
        user.changeAccountStatus(newStatus);

        userRepository.update(user);
    }

    // admin
    public void approveUser(long userId, Role role) {
        validateUserId(userId);

        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        if (role == Role.UNASSIGNED) {
            throw new IllegalArgumentException("Approved user must have a role.");
        }

        User user = findUserByUserId(userId);

        user.changeRole(role);
        user.changeAccountStatus(AccountStatus.ACTIVE);

        userRepository.update(user);
    }

    public void rejectUser(long userId) {
        validateUserId(userId);

        User user = findUserByUserId(userId);
        user.changeAccountStatus(AccountStatus.REJECTED);

        userRepository.update(user);
    }

    public void deactivateUser(long userId) {
        validateUserId(userId);

        User user = findUserByUserId(userId);
        user.changeAccountStatus(AccountStatus.DISABLED);

        userRepository.update(user);
    }

    private void validateUserId(long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be empty.");
        }
    }

    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }
    }

}