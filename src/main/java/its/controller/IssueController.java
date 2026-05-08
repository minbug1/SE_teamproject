package its.controller;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Status;
import its.model.User;
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

    public Issue reportIssue(String title, String description, User reporter, Priority priority, Comment comment) {
        
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
        if (comment != null) {
            if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Comment content must not be empty.");
            }
            if (comment.getAuthor() == null) {
                throw new IllegalArgumentException("Comment author must not be null.");
            }
            if (comment.getWrittenDate() == null) {
                throw new IllegalArgumentException("Comment written date must not be null.");
            }
        }

        Issue newIssue = new Issue(
                "issue1", // todo: ID 자동 생성
                title,
                description,
                reporter,
                LocalDateTime.now()
        );

        newIssue.setPriority(priority);
        if (comment != null) {
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
}
