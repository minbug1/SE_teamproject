package its.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Issue {

    private int issueId;
    private String title;
    private String description;
    private User reporter;
    private LocalDateTime reportedDate;
    private User assignee;
    private User fixer;
    private Priority priority;
    private Status status;
    private List<Comment> comments = new ArrayList<>();

    public Issue(int issueId, String title, String description, User reporter, LocalDateTime reportedDate) {
        this.issueId = issueId;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.reportedDate = reportedDate;
        this.priority = Priority.MAJOR; 
        this.status = Status.NEW; 
    }

    public int getIssueId() {
        return issueId;
    }
    
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public User getReporter() {
        return reporter;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }   

    public User getAssignee() {
        return assignee;
    }

    public Status getStatus() {
        return status;
    }

    public User getFixer() {
        return fixer;
    }

    public Priority getPriority() {
        return priority;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public List<Comment> getComments() {
        return comments;
    }


}
