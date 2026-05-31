package its.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Issue {

    private long issueId;
    private int projectId;
    private String title;
    private String description;
    private Priority priority;
    private IssueStatus status;
    private User reporter;
    private User assignee;
    private User fixer;
    private LocalDateTime reportedDate;
    private LocalDateTime assignedDate;
    private LocalDateTime fixedDate;
    private LocalDateTime resolvedDate;
    private LocalDateTime closedDate;
    private List<LocalDateTime> reopenedDates = new ArrayList<>();
    private int reopenCount;
    private int categoryId;
    private List<Comment> comments = new ArrayList<>();

    public Issue(long issueId,int projectId, String title, String description, User reporter, LocalDateTime reportedDate) {
        this.issueId = issueId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.reportedDate = reportedDate;
        this.reopenCount = 0;
        this.priority = Priority.MAJOR; 
        this.status = IssueStatus.NEW;
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

    public Priority getPriority() {
        return priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public User getReporter() {
        return reporter;
    }   

    public User getAssignee() {
        return assignee;
    }

    public User getFixer() {
        return fixer;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }  

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public LocalDateTime getFixedDate() {
        return fixedDate;
    }

    public LocalDateTime getResolvedDate() {
        return resolvedDate;
    }

    public LocalDateTime getClosedDate() {
        return closedDate;
    }

    public List<LocalDateTime> getReopenedDates() {
        return new ArrayList<>(reopenedDates);
    }

    public int getReopenCount() {
        return reopenCount;
    }


    public int getCategoryId() {
        return categoryId;
    }

    //comment 새로 생성할때 commentid = 현재 comment개수+1
    public int getNextCommentId() {
        return this.comments.size() + 1;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
    
    public void setFixer(User fixer) {
        this.fixer = fixer;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public void setFixedDate(LocalDateTime fixedDate) {
        this.fixedDate = fixedDate;
    }

    public void setResolvedDate(LocalDateTime resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public void setClosedDate(LocalDateTime closedDate) {
        this.closedDate = closedDate;
    }

    public void setReopenCount(int reopenCount) {
        this.reopenCount = reopenCount;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setReopenedDates(List<LocalDateTime> reopenedDates) {
        this.reopenedDates = reopenedDates;
    }

    public List<Comment> getComments() {
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        return new ArrayList<>(this.comments);
    }

    public void incrementReopenCount() {
        this.reopenCount++;
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
