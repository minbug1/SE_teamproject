package its.controller;

import its.model.AccountStatus;
import its.model.Role;
import its.model.User;
import its.repository.FileUserRepository;
import its.repository.UserRepository;

import java.util.List;

/**
 * controller for user account management (logged in)
 * create, delete, find, change, approve/reject/deactivate, helper
 *
 * @author hanung
 */

public class UserController {

    private final UserRepository userRepository;

    public UserController() {
        this(new FileUserRepository());
    }

    // constructor for test(repository replacement)
    public UserController(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null.");
        }

        this.userRepository = userRepository;
    }

    // create // user register 없이 admin이 직접 생성, Q : accountStatus Active default
    public User createUserByAdmin(User currentUser, String loginId, String password, AccountStatus accountStatus, Role role) {
        validateAdmin(currentUser);

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

    // delete // user delete는 admin만
    public void deleteUser(User currentUser, long userId) {
        validateAdmin(currentUser);
        validateUserId(userId);

        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("User does not exist.");
        }

        userRepository.deleteByUserId(userId);
    }

    // find validation helper
    private User getUserByUserId(long userId) {
        validateUserId(userId);

        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        return user;
    }

    // find // login 요구
    public User findUserByUserId(User currentUser, long userId) {
        validateLogin(currentUser);
        
        return getUserByUserId(userId);
    }

    public User findUserByLoginId(User currentUser, String loginId) { // 당장 필요하진 않음
        validateLogin(currentUser);
        validateLoginId(loginId);

        User user = userRepository.findByLoginId(loginId);

        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        return user;
    }

    public List<User> findUsersByAccountStatus(User currentUser, AccountStatus accountStatus) {
        validateAdmin(currentUser);

        if (accountStatus == null) {
            throw new IllegalArgumentException("Account Status must not be null");
        }

        return userRepository.findByAccountStatus(accountStatus);
    }

    public List<User> findUsersByRole(User currentUser, Role role) {
        validateLogin(currentUser);

        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        return userRepository.findByRole(role);
    }

    public List<User> findAllUsers(User currentUser) {
        validateLogin(currentUser);

        return userRepository.findAll();
    }
    
    // change //
    public void changeLoginId(User currentUser, String newLoginId) {
        validateLogin(currentUser);
        validateLoginId(newLoginId);

        User duplicatedUser = userRepository.findByLoginId(newLoginId);

        if (duplicatedUser != null && duplicatedUser.getUserId() != currentUser.getUserId()) {
            throw new IllegalArgumentException("Login ID already exists.");
        }

        User user = getUserByUserId(currentUser.getUserId());
        user.changeLoginId(newLoginId);

        userRepository.update(user);
    }

    public void changePassword(User currentUser, String newPassword) {
        validateLogin(currentUser);

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        User user = getUserByUserId(currentUser.getUserId());
        user.changePassword(newPassword);

        userRepository.update(user);
    }

     public void changeRole(User currentUser, long userId, Role newRole) {
        validateAdmin(currentUser);
        validateUserId(userId);

        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        User user = getUserByUserId(userId);
        user.changeRole(newRole);

        userRepository.update(user);
    }

    public void changeAccountStatus(User currentUser, long userId, AccountStatus newStatus) {
        validateAdmin(currentUser);
        validateUserId(userId);

        if (newStatus == null) {
            throw new IllegalArgumentException("Account status must not be null.");
        }

        User user = getUserByUserId(userId);
        user.changeAccountStatus(newStatus);

        userRepository.update(user);
    }

    // admin helper // 없어도 되나 명시적으로 지원
    public List<User> findPendingUsers(User currentUser) {
        validateAdmin(currentUser);

        return userRepository.findPendingUsers();
    }

    public void approveUser(User currentUser, long userId, Role role) {
        validateAdmin(currentUser);
        validateUserId(userId);

        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        if (role == Role.UNASSIGNED) {
            throw new IllegalArgumentException("Approved user must have a role.");
        }

        User user = getUserByUserId(userId);

        user.changeRole(role);
        user.changeAccountStatus(AccountStatus.ACTIVE);

        userRepository.update(user);
    }

    public void rejectUser(User currentUser, long userId) {
        validateAdmin(currentUser);
        validateUserId(userId);

        User user = getUserByUserId(userId);
        user.changeAccountStatus(AccountStatus.REJECTED);

        userRepository.update(user);
    }

    public void deactivateUser(User currentUser, long userId) {
        validateAdmin(currentUser);
        validateUserId(userId);

        User user = getUserByUserId(userId);
        user.changeAccountStatus(AccountStatus.DISABLED);

        userRepository.update(user);
    }

    // 비밀번호 초기화, 임시 구현(Auth에 추가 필요), '0000' 오버로딩
    public void resetPasswordByAdmin(User currentUser, long userId) {
        resetPasswordByAdmin(currentUser, userId, "0000");
    }

    public void resetPasswordByAdmin(User currentUser, long userId, String temporaryPassword) {
        validateAdmin(currentUser);
        validateUserId(userId);

        if (temporaryPassword == null || temporaryPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Temporary password must not be empty.");
        }

        User user = getUserByUserId(userId);
        user.changePassword(temporaryPassword);

        userRepository.update(user);
    }

    // helper //
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }
    }

    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }
    }

    private void validateLogin(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Login is required.");
        }
    }

    private void validateAdmin(User currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalArgumentException("Admin permission is required.");
        }
    }

}
