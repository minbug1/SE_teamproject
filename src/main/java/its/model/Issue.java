package its.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Issue {
    
    private String issueId;
    private String title;
    private String description;
    private User reporter;
    private LocalDateTime reportedDate;
    private User assignee;
    private User fixer;
    private Priority priority;
    private Status status;
    private List<Comment> comments = new ArrayList<>();

    public Issue(String issueId, String title, String description, User reporter, LocalDateTime reportedDate) {
        this.issueId = issueId;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.reportedDate = reportedDate;
        this.priority = Priority.MAJOR; 
        this.status = Status.NEW; 
    }

}
