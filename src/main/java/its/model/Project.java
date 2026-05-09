package its.model;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private int projectId;
    private String name;
    private String description;
    private List<User> members = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();

    public Project(int projectId, String name, String description) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
    }
    
}
