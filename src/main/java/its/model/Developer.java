package its.model;

public class Developer extends User {

    public Developer(Long userId, String loginId, String password) {
        super(userId, loginId, password);
    }
    
    @Override
    public Role getRole() {
        return Role.DEVELOPER;
    }
}
