package its.model;

public class Tester extends User {

    public Tester(String id, String name, String password) {
        super(id, name, password);
    }
    
    @Override
    public Role getRole() {
        return Role.TESTER;
    }
}
