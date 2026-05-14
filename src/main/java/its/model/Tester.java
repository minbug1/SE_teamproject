package its.model;

public class Tester extends User {

    public Tester(Long userId, String loginId, String password) {
        super(userId, loginId, password);
    }
    
    @Override
    public Role getRole() {
        return Role.TESTER;
    }
}
