package its.model;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private int projectId;
    private String name;
    private String description;
    private List<User> members = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
    private List<Long> memberIds = new ArrayList<>();
    private List<Long> issueIds = new ArrayList<>();

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
        if (issue != null && issue.getIssueId() != 0) {
            long issueId = issue.getIssueId();
            boolean alreadyExists = getIssues().stream()
                    .anyMatch(existing -> existing.getIssueId() != 0
                            && existing.getIssueId() == issueId);

            if (!alreadyExists) {
                this.issues.add(issue);
            }
            addIssueId(issueId);
        }
    }

    public void addIssueId(long issueId) {
        if (issueId > 0 && !getIssueIds().contains(issueId)) {
            getIssueIds().add(issueId);
        }
    }

    public void removeIssueById(long issueId) {
        getIssues().removeIf(issue -> issue.getIssueId() != 0
                && issue.getIssueId() == issueId);
        getIssueIds().removeIf(id -> id != 0 && id == issueId);
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

    public List<Long> getMemberIds() { 
        return memberIds; 
    }
    public List<Long> getIssueIds() { 
        if (this.issueIds == null) {
            this.issueIds = new ArrayList<>();
        }
        return issueIds; 
    }
}

