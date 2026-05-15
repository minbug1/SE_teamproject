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
    
    public void addMember(User user) {
        if (user != null && !members.contains(user)) { // 중복 추가 방지
            this.members.add(user);
        }
    }

    public void addIssue(Issue issue) {
        if (issue != null) { 
            this.issues.add(issue);
        }
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<User> getMembers() {
        return members;
    }

    public List<Issue> getIssues() {
        return issues;
    }
}

