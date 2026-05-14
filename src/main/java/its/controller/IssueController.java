package its.controller;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Status;
import its.model.User;
import its.model.Tester;
import its.model.PL;
import its.model.Developer;
import its.repository.FileIssueRepository;
import its.repository.IssueRepository;

import java.time.LocalDateTime;

public class IssueController {

    private IssueRepository issueRepository;

    public IssueController() {
        this(new FileIssueRepository());
    }

    public IssueController(IssueRepository issueRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }
        this.issueRepository = issueRepository;
    }

    public Issue reportIssue(String title, String description, User reporter, Priority priority, String commentContent) {
        
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

        Issue newIssue = new Issue(
                1, // todo: ID 자동 생성
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
    public Issue updateState(int issueId, String commentContent, Developer dev){
        Issue issue = issueRepository.findById(issueId);

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

        issue.setFixer(dev)
        
        return issue;
    }

    //Verify Issue
    public Issue updateState(int issueId, String commentContent, Tester tester, boolean isResolved){
        Issue issue = issueRepository.findById(issueId);

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

        return issue;
    }

    //Close Issue
    public Issue updateState(int issueId, String commentContent, PL pl){
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
        
        return issue;
    }

    //Assign Issue 이거 조금더 수정 필요함
    public Issue assignIssue(int issueId, Developer assignee, PL pl, String commentContent) {
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

        return issue;
    }
}
