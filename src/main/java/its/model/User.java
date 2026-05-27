
package its.model;

import java.util.Objects;

/*
 * model for user
 * 
 *
 * @author hanung
 */

public class User {

    private final long userId;
    private String loginId;
    private String password;
    private AccountStatus accountStatus;
    private UserRole role;

    // constructor 
    public User(Long userId, String loginId, String password) {
       this(userId, loginId, password, AccountStatus.PENDING, UserRole.UNASSIGNED);
    }

    public User(Long userId, String loginId, String password, AccountStatus accountStatus, UserRole role) {
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
    public long getUserId() {
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

    public UserRole getRole() {
        return role;
    }

    // set
    public void setLoginId(String newLoginId) {
        if (newLoginId == null || newLoginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID must not be empty.");
        }

        this.loginId = newLoginId;
    }

    public void setPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        this.password = newPassword;
    }

    public void setAccountStatus(AccountStatus newAccountStatus) {
        if (newAccountStatus == null) {
            throw new IllegalArgumentException("Account Status must not be null.");
        }

        this.accountStatus = newAccountStatus;
    }

    public void setRole(UserRole newRole) {
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

    public boolean isPending() {
        return accountStatus == AccountStatus.PENDING;
    }

    public boolean isRejected() {
        return accountStatus == AccountStatus.REJECTED;
    }

    public boolean isDisabled() {
        return accountStatus == AccountStatus.DISABLED;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isPL() {
        return role == UserRole.PL;
    }

    public boolean isDev() {
        return role == UserRole.DEVELOPER;
    }

    public boolean isTester() {
        return role == UserRole.TESTER;
    }

    public boolean isUnassigned() {
        return role == UserRole.UNASSIGNED;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User)) {
            return false;
        }

        User other = (User) obj;
        return this.userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
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