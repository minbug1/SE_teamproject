package its.controller;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.Status;
import its.model.User;
import its.model.Tester;
import its.model.PL;
import its.model.Developer;
import its.repository.FileIssueRepository;
import its.repository.IssueRepository;

import java.time.LocalDateTime;
import java.util.List;

public class IssueController {

    private IssueRepository issueRepository;

    // 기본 생성자에서 FileIssueRepository를 사용하도록 설정
    public IssueController() {
        this(new FileIssueRepository());
    }

    // 의존성 주입을 위한 생성자
    public IssueController(IssueRepository issueRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }
        this.issueRepository = issueRepository;
    }

    //Report Issue
    public Issue reportIssue(Project project, String title, String description, User reporter, Priority priority, String commentContent) {
        
        //user가 project 멤버인지
        if (project == null || !project.getMembers().contains(reporter)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 이슈를 등록할 권한이 없습니다.");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description must not be empty.");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("Reporter must not be null.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority must not be null.");
        }

        int newIssueId = issueRepository.findAll().size() + 1;
        Issue newIssue = new Issue(
                newIssueId,
                title,
                description,
                reporter,
                LocalDateTime.now()
        );

        newIssue.setPriority(priority);
        if (commentContent != null && !commentContent.trim().isEmpty()) {
            Comment comment = new Comment(
                    1, // todo: ID 자동 생성
                    commentContent,
                    reporter,
                    LocalDateTime.now()
            );
            newIssue.addComment(comment);
        }



        newIssue.setStatus(Status.NEW);

        // db에 이슈 저장
        issueRepository.save(newIssue);

        return newIssue;
        /*
        View 에서

        createButton.addActionListener(e -> {

        Issue created = controller.reportIssue(title, desc, currentUser, priority, comment);

        issueDetailPanel.show(created);   
        issueList.add(created);    

        });
        */
    }
    //

    //Fix Issue
    public Issue fixIssue(Project project, int issueId, String commentContent, User dev){
        Issue issue = issueRepository.findById(issueId);

        //user가 project 멤버인지
        if (project == null || !project.getMembers().contains(dev)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 이슈를 수정할 권한이 없습니다.");
        }
        //조건 Assigned된 이슈
        if (issue == null || issue.getStatus() != Status.ASSIGNED) {
            return null;
        }

        // 코멘트 객체 생성 및 이슈에 추가
        if (commentContent != null && !commentContent.isEmpty()) {
            int nextCommentId = issue.getComments().size() + 1;
            Comment comment = new Comment(nextCommentId, commentContent, dev, LocalDateTime.now());
            issue.addComment(comment);
        }

        // 상태 변경
        issue.setStatus(Status.FIXED);

        issue.setFixer(dev);

        issueRepository.update(issue);
        
        return issue;
    }

    //Verify Issue
    public Issue verifyIssue(Project project, int issueId, String commentContent, User tester, boolean isResolved){
        Issue issue = issueRepository.findById(issueId);

        
        //user가 project 멤버인지
        if (project == null || !project.getMembers().contains(tester)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 이슈를 수정할 권한이 없습니다.");
        }

        //조건 FIXED된 이슈
        if (issue == null || issue.getStatus() != Status.FIXED) {
            return null;
        }

        if (isResolved) {
            // 해결 성공
            issue.setStatus(Status.RESOLVED);
        } else {
            // 해결 실패 
            issue.setStatus(Status.REOPENED);
        }

        // 코멘트 객체 생성 및 이슈에 추가
        if (commentContent != null && !commentContent.isEmpty()) {
            int nextCommentId = issue.getComments().size() + 1;
            Comment comment = new Comment(nextCommentId, commentContent, tester, LocalDateTime.now());
            issue.addComment(comment);
        }

        issueRepository.update(issue);
        
        return issue;
    }

    //Close Issue
    public Issue closeIssue(Project project, int issueId, String commentContent, User pl){

        //user가 project 멤버인지
        if (project == null || !project.getMembers().contains(pl)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 이슈를 수정할 권한이 없습니다.");
        }

        Issue issue = issueRepository.findById(issueId);
        

        //조건 Resolved된 이슈
        if (issue == null || issue.getStatus() != Status.RESOLVED) {
            return null;
        }

        // 코멘트 객체 생성 및 이슈에 추가
        if (commentContent != null && !commentContent.isEmpty()) {
            int nextCommentId = issue.getComments().size() + 1;
            Comment comment = new Comment(nextCommentId, commentContent, pl, LocalDateTime.now());
            issue.addComment(comment);
        }

        // 상태 변경
        issue.setStatus(Status.CLOSED);
        
        issueRepository.update(issue);

        return issue;
    }

    //Assign Issue 이거 조금더 수정 필요함
    public Issue assignIssue(Project project, int issueId, User assignee, User pl, String commentContent) {

        // 권한 및 유효성 검증
        if (project == null) {
         throw new IllegalArgumentException("프로젝트 정보가 없습니다.");
        }
        if (!project.getMembers().contains(pl)) {
            throw new SecurityException("이 프로젝트의 PL이 아닙니다.");
        }
        if (!project.getMembers().contains(assignee)) {
            throw new IllegalArgumentException("할당하려는 개발자가 프로젝트 멤버가 아닙니다.");
        }

        Issue issue = issueRepository.findById(issueId);

        // NEW 또는 REOPENED 상태일 때 할당이 가능합니다.
        if (issue == null || (issue.getStatus() != Status.NEW && issue.getStatus() != Status.REOPENED)) {
            return null;
        }

        // PL 및 Assignee 유효성 검사
        if (pl == null || assignee == null) {
            throw new IllegalArgumentException("PL and Assignee must not be null.");
        }

        // 담당자 설정 및 상태 변경
        issue.setAssignee(assignee);
        issue.setStatus(Status.ASSIGNED); 

        //코멘트 객체 생성 및 이슈에 추가
        if (commentContent != null && !commentContent.trim().isEmpty()) {
            int nextCommentId = issue.getComments().size() + 1;
            Comment comment = new Comment(nextCommentId, commentContent, pl, LocalDateTime.now());
            issue.addComment(comment); 
        }

        issueRepository.update(issue);

        return issue;
    }

    // 이슈 삭제
    public void deleteIssue(Project project, int issueId, User user) {
        
        // 1. 권한 검증 (예: 프로젝트 멤버만 삭제 가능하다고 가정)
        if (project == null || !project.getMembers().contains(user)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 이슈를 삭제할 수 없습니다.");
        }

        // 2. Project 객체가 가진 이슈 리스트에서 해당 이슈 제거
        project.getIssues().removeIf(issue -> issue.getIssueId() == issueId);

        // 3. IssueRepository를 통해 이슈 데이터 완전히 삭제 (파일에서 지워짐)
        issueRepository.delete(issueId);

    }

    //이거 UI구현되면 수정필요
    public void showIssues(Project project, Status filterStatus) {
        if (project == null) {
            System.out.println("❌ 프로젝트 정보가 없습니다.");
            return;
        }
    

        String statusText = (filterStatus == null) ? "전체" : filterStatus.toString();
        System.out.println("\n===== [" + project.getName() + "] 이슈 목록 (" + statusText + ") =====");

        List<Issue> issues = project.getIssues();
    
        // 이슈가 하나도 없는 경우
        if (issues.isEmpty()) {
            System.out.println("   접수된 이슈가 없습니다.");
            return;
        }

        boolean found = false;
        for (Issue issue : issues) {
            // filterStatus가 null이면 무조건 출력, 아니면 상태가 일치하는 것만 출력
            if (filterStatus == null || issue.getStatus() == filterStatus) {
                issue.printIssueInfo(); // 지난번에 만든 출력 메소드 활용
                found = true;
            }
        }
    

        if (!found) {
            System.out.println("   해당 상태의 이슈가 없습니다.");
        }
        System.out.println("==========================================\n");
    }
}
