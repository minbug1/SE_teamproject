package its.controller;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Status;
import its.model.Tester;
import its.repository.FileIssueRepository;

import java.time.LocalDateTime;

public class IssueController {

    private FileIssueRepository issueRepository;

    public Issue createIssue(String title, String description, Tester reporter, Priority priority, Comment comment) {

        // 이슈 등록 (꼭 채워야하는 필드는 Title, Description, reporter 필드는 이슈를 등록한 계정으로, 
        // reported date는 등록한 날짜와 시간으로 자동으로 채워짐)

        Issue newIssue = new Issue(
                "issue1", // todo: ID 자동 생성
                title,
                description,
                reporter,
                LocalDateTime.now()
        );

        newIssue.setPriority(priority);
        newIssue.addComment(comment);

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