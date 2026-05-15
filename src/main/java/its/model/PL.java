package its.model;

public class PL extends User {

    public PL(Long userId, String loginId, String password) {
        super(userId, loginId, password, AccountStatus.ACTIVE, Role.PL);
    }
    
    @Override
    public Role getRole() {
        return Role.PL;
    }
}
