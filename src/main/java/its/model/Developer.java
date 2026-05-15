package its.model;

public class Developer extends User {

    public Developer(Long userId, String loginId, String password) {
        super(userId, loginId, password, AccountStatus.ACTIVE, Role.DEVELOPER);
    }
    
    @Override
    public Role getRole() {
        return Role.DEVELOPER;
    }
}
