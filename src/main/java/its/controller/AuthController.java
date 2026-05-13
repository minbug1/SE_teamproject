
package its.controller;


import its.model.User;
import its.repository.FileUserRepository;
import its.repository.UserRepository;

/**
 * controller for authentication
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
            throw new IllegalArgumentException("Account is not active.");
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
}