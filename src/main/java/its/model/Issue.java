package its.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Issue {

    private long issueId;
    private int projectId;
    private String title;
    private String description;
    private User reporter;
    private LocalDateTime reportedDate;
    private User assignee;
    private User fixer;
    private Priority priority;
    private Status status;
    private List<Comment> comments = new ArrayList<>();

    public Issue(long issueId,int projectId, String title, String description, User reporter, LocalDateTime reportedDate) {
        this.issueId = issueId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.reportedDate = reportedDate;
        this.priority = Priority.MAJOR; 
        this.status = Status.NEW;
    }

    public long getIssueId() {
        return issueId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
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

    //comment 새로 생성할때 commentid = 현재 comment개수+1
    public int getNextCommentId() {
        return this.comments.size() + 1;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }
    
    public void setFixer(User fixer) {
        this.fixer = fixer;
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
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        return new ArrayList<>(this.comments);
    }

    // 여거 테스트용도
    public void printIssueInfo() {
        System.out.println("--------------------------------------------------");
        System.out.println("🔹 [이슈 #" + this.issueId + "] " + this.title);
        System.out.println("   - 현재 상태: " + this.status + " | 우선순위: " + this.priority);
        System.out.println("   - 생성자(Reporter): " + (this.reporter != null ? this.reporter.getLoginId() : "없음"));
        System.out.println("   - 담당자(Assignee): " + (this.assignee != null ? this.assignee.getLoginId() : "없음"));
        System.out.println("   - 해결자(Fixer): " + (this.fixer != null ? this.fixer.getLoginId() : "없음"));
        System.out.println("   - 설명: " + this.description);
        System.err.println("   - 보고된 날짜: " + this.reportedDate);

        // 코멘트가 있다면 모두 출력
        if (this.comments != null && !this.comments.isEmpty()) {
            System.out.println("   - 코멘트 내역:");
            for (Comment c : this.comments) {
                // Comment 클래스에 getter가 있다고 가정
                System.out.println("      └ [" + c.getAuthor().getLoginId() + "] " + c.getContent());
            }
        }
        System.out.println("--------------------------------------------------\n");
    }

}
