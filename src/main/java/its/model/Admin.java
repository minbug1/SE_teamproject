package its.model;

public class Admin extends User {

    public Admin(Long userId, String loginId, String password) {
        super(userId, loginId, password, AccountStatus.ACTIVE, Role.ADMIN);
    }
    
    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}

