package its.controller;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.Status;
import its.model.Role;
import its.model.User;
import its.repository.FileIssueRepository;
import its.repository.IssueRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    public Issue reportIssue(Project project, String title, String description, User reporter, Priority priority, String commentContent) {
        
        if (project == null) {
            throw new IllegalArgumentException("이슈가 속할 프로젝트 정보가 없습니다.");
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

        Issue newIssue = new Issue(
                issueRepository.generateIssueId(),
                title,
                description,
                reporter,
                LocalDateTime.now()
        );

        newIssue.setPriority(priority);
        newIssue.setStatus(Status.NEW);
        if (commentContent != null && !commentContent.trim().isEmpty()) {
            Comment comment = new Comment(
                    newIssue.getNextCommentId(),
                    commentContent,
                    reporter,
                    LocalDateTime.now()
            );
            newIssue.addComment(comment);
        }



        newIssue.setStatus(Status.NEW);

        project.addIssue(newIssue);

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

    // Fix Issue
    public Issue fixIssue(Project project, int issueId, String commentContent, User dev) {

        validateProject(project);
        validateUser(dev, "Developer must not be null.");
        validateRole(dev, Role.DEVELOPER, "Only developers can fix issues.");
        validateProjectMember(project, dev, "Need to be a member of the project to fix the issue.");

        Issue issue = getIssueOrNull(issueId);

        if (issue == null || issue.getStatus() != Status.ASSIGNED) {
            return null;
        }

        if (issue.getAssignee() == null || !issue.getAssignee().equals(dev)) {
            throw new SecurityException("Only assigned issues can be fixed.");
        }

        addCommentIfPresent(issue, commentContent, dev);

        issue.setStatus(Status.FIXED);
        issue.setFixer(dev);

        issueRepository.update(issue);

        return issue;
    }

    // Verify Issue
    public Issue verifyIssue(Project project, int issueId, String commentContent, User tester, boolean isResolved) {

        validateProject(project);
        validateUser(tester, "Tester must not be null.");
        validateRole(tester, Role.TESTER, "Only testers can verify issues.");
        validateProjectMember(project, tester, "Need to be a member of the project to verify the issue.");

        Issue issue = getIssueOrNull(issueId);

        if (issue == null || issue.getStatus() != Status.FIXED) {
            return null;
        }

        if (issue.getReporter() == null || !issue.getReporter().equals(tester)) {
            throw new SecurityException("Only the reporter can verify the issue.");
        }

        if (isResolved) {
            issue.setStatus(Status.RESOLVED);
        } else {
            issue.setStatus(Status.REOPENED);
        }

        addCommentIfPresent(issue, commentContent, tester);

        issueRepository.update(issue);

        return issue;
    }

    // Close Issue
    public Issue closeIssue(Project project,  int issueId, String commentContent, User pl) {

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateRole(pl, Role.PL, "Only PL can change the issue status to closed.");
        validateProjectMember(project, pl, "Need to be a member of the project to close the issue.");

        Issue issue = getIssueOrNull(issueId);

        if (issue == null || issue.getStatus() != Status.RESOLVED) {
            return null;
        }

        addCommentIfPresent(issue, commentContent, pl);

        issue.setStatus(Status.CLOSED);

        issueRepository.update(issue);

        return issue;
    }

    // Assign Issue
    public Issue assignIssue(Project project, int issueId, User assignee, User pl, String commentContent) {

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateUser(assignee, "Assignee must not be null.");

        validateRole(pl, Role.PL, "Only PL can assign issues.");
        validateRole(assignee, Role.DEVELOPER, "Only developers can be assigned to issues.");

        validateProjectMember(project, pl, "This user is not a PL member of the project.");
        validateProjectMember(project, assignee, "The assignee is not a member of the project.");

        Issue issue = getIssueOrNull(issueId);

        if (issue == null || (issue.getStatus() != Status.NEW && issue.getStatus() != Status.REOPENED)) {
            return null;
        }

        issue.setAssignee(assignee);
        issue.setStatus(Status.ASSIGNED);

        addCommentIfPresent(issue, commentContent, pl);

        issueRepository.update(issue);

        return issue;
    }

    // helper
    private Issue getIssueOrNull(int issueId) {
        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be a positive number.");
        }

        return issueRepository.findById(issueId);
    }

    private void addCommentIfPresent(Issue issue, String commentContent, User author) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue must not be null.");
        }

        if (author == null) {
            throw new IllegalArgumentException("Author must not be null.");
        }

        if (commentContent == null || commentContent.trim().isEmpty()) {
            return;
        }

        Comment comment = new Comment(
                issue.getNextCommentId(),
                commentContent,
                author,
                LocalDateTime.now()
        );

        issue.addComment(comment);
    }

    private void validateProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("프로젝트 정보가 없습니다.");
        }
    }

    private void validateUser(User user, String message) {
        if (user == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateRole(User user, Role requiredRole, String message) {
        validateUser(user, "User must not be null.");

        if (requiredRole == null) {
            throw new IllegalArgumentException("Required role must not be null.");
        }

        if (user.getRole() != requiredRole) {
            throw new SecurityException(message);
        }
    }

    private void validateProjectMember(Project project, User user, String message) {
        validateProject(project);
        validateUser(user, "User must not be null.");

        if (!project.getMembers().contains(user)) {
            throw new SecurityException(message);
        }
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
