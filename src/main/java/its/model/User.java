
package its.model;

/**
 * model for user
 * 
 *
 * @author hanung
 */

public class User {

    private final Long userId;
    private String loginId;
    private String password;
    private AccountStatus accountStatus;
    private Role role;

    // constructor
    public User(Long userId, String loginId, String password) {
        this(userId, loginId, password, AccountStatus.PENDING, Role.UNASSIGNED);
    }

    public User(Long userId, String loginId, String password, AccountStatus accountStatus, Role role) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be empty.");
        }

        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        if (accountStatus == null) {
            throw new IllegalArgumentException("Account Status must not be null.");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.accountStatus = accountStatus;
        this.role = role;
    }

    // get
    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Role getRole() {
        return role;
    }

    // change
    public void changeLoginId(String newLoginId) {
        if (newLoginId == null || newLoginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }

        this.loginId = newLoginId;
    }

    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        this.password = newPassword;
    }

    public void changeAccountStatus(AccountStatus newAccountStatus) {
        if (newAccountStatus == null) {
            throw new IllegalArgumentException("Account Status must not be null.");
        }

        this.accountStatus = newAccountStatus;
    }

    public void changeRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }

        this.role = newRole;
    }

    // verify
    public boolean matchesPassword(String password) {
        if (password == null) {
            return false;
        }

        return this.password.equals(password);
    }

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isPL() {
        return role == Role.PL;
    }

    public boolean isDev() {
        return role == Role.DEVELOPER;
    }

    public boolean isTester() {
        return role == Role.TESTER;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", loginId='" + loginId + '\'' +
                ", accountStatus=" + accountStatus +
                ", role=" + role +
                '}';
    }
}