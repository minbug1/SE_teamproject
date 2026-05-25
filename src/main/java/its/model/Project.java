package its.model;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private int projectId;
    private String name;
    private String description;
    private List<User> members = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
    private List<Integer> memberIds = new ArrayList<>();
    private List<Integer> issueIds = new ArrayList<>();

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
        if (issue != null && issue.getIssueId() != null) {
            int issueId = issue.getIssueId().intValue();
            boolean alreadyExists = getIssues().stream()
                    .anyMatch(existing -> existing.getIssueId() != null
                            && existing.getIssueId().intValue() == issueId);

            if (!alreadyExists) {
                this.issues.add(issue);
            }
            addIssueId(issueId);
        }
    }

    public void addIssueId(int issueId) {
        if (issueId > 0 && !getIssueIds().contains(issueId)) {
            getIssueIds().add(issueId);
        }
    }

    public void removeIssueById(int issueId) {
        getIssues().removeIf(issue -> issue.getIssueId() != null
                && issue.getIssueId().intValue() == issueId);
        getIssueIds().removeIf(id -> id != null && id == issueId);
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
        // 만약 리스트가 null 상태라면 텅 빈 리스트를 새로 만들어줍니다.
        if (this.members == null) {
            this.members = new ArrayList<>();
        }
        return this.members;
    }

    public List<Issue> getIssues() {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        return this.issues;
    }

    public List<Integer> getMemberIds() { 
        return memberIds; 
    }
    public List<Integer> getIssueIds() { 
        if (this.issueIds == null) {
            this.issueIds = new ArrayList<>();
        }
        return issueIds; 
    }
}

