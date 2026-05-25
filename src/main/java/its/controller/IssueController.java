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
import its.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class IssueController {

    private IssueRepository issueRepository;
    private ProjectRepository projectRepository;

    // 기본 생성자에서 FileIssueRepository를 사용하도록 설정
    public IssueController() {
        this(new FileIssueRepository());
    }

    // 의존성 주입을 위한 생성자
    public IssueController(IssueRepository issueRepository) {
        this(issueRepository, null);
    }

    public IssueController(IssueRepository issueRepository, ProjectRepository projectRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    //Report Issue
    public Issue reportIssue(Project project, String title, String description, User reporter, Priority priority, String commentContent) {
        
        
        // 기존 이슈 ID 목록
        for (Issue i : issueRepository.findAll()) {
            System.out.println("  기존 이슈 id=" + i.getIssueId() + " projectId=" + i.getProjectId());
        }

        //user가 project 멤버인지
        validateMember(project, reporter);

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

        long newIssueId = generateIssueIdInProject(project);
        int projectId = project.getProjectId();

        Issue newIssue = new Issue(newIssueId, projectId, title, description, reporter, LocalDateTime.now());

        newIssue.setProjectId(project.getProjectId());

        newIssue.setPriority(priority);
        newIssue.setStatus(Status.NEW);

        if (commentContent != null && !commentContent.trim().isEmpty()) {
            Comment comment = new Comment(
                    newIssue.getNextCommentId(), commentContent, reporter, LocalDateTime.now());
            newIssue.addComment(comment);
        }

        issueRepository.save(newIssue);

        project.addIssue(newIssue);
        updateProjectIfAvailable(project);
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
    public Issue fixIssue(Project project, long issueId, String commentContent, User dev){

        validateMember(project, dev);
        validateIssueId(issueId);
        
        Issue issue = getProjectIssueOrNull(project, issueId);
        validateIssueStatus(issue, Status.ASSIGNED);

        if (issue.getAssignee() == null || !issue.getAssignee().equals(dev)) {
            throw new SecurityException("Only assigned issues can be fixed.");
        }

        addCommentIfPresent(issue, commentContent, dev);

        issue.setStatus(Status.FIXED);
        issue.setFixer(dev);

        issueRepository.update(issue);
        
        return issue;
    }

    //Verify Issue
    public Issue verifyIssue(Project project, long issueId, String commentContent, User tester, boolean isResolved){

        validateProject(project);
        validateUser(tester, "Tester must not be null.");
        validateRole(tester, Role.TESTER, "Only testers can verify issues.");
        validateProjectMember(project, tester, "Need to be a member of the project to verify the issue.");

        Issue issue = getProjectIssueOrNull(project, issueId);

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

    //Close Issue
    public Issue closeIssue(Project project, long issueId, String commentContent, User pl){

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateRole(pl, Role.PL, "Only PL can change the issue status to closed.");
        validateProjectMember(project, pl, "Need to be a member of the project to close the issue.");

        Issue issue = getProjectIssueOrNull(project, issueId);

        if (issue == null || issue.getStatus() != Status.RESOLVED) {
            return null;
        }

        addCommentIfPresent(issue, commentContent, pl);

        issue.setStatus(Status.CLOSED);
        
        issueRepository.update(issue);

        return issue;
    }

    //Assign Issue 이거 조금더 수정 필요함
    public Issue assignIssue(Project project, long issueId, User assignee, User pl, String commentContent) {

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateUser(assignee, "Assignee must not be null.");

        validateRole(pl, Role.PL, "Only PL can assign issues.");
        validateRole(assignee, Role.DEVELOPER, "Only developers can be assigned to issues.");

        validateProjectMember(project, pl, "This user is not a PL member of the project.");
        validateProjectMember(project, assignee, "The assignee is not a member of the project.");

        Issue issue = getProjectIssueOrNull(project, issueId);

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
    private long generateIssueIdInProject(Project project) {
        long maxId = 0;
        for (Long issueId : project.getIssueIds()) {
            if (issueId != null && issueId > maxId) {
                maxId = issueId;
            }
        }
        for (Issue issue : project.getIssues()) {
            if (issue.getIssueId() != 0 && issue.getIssueId() > maxId) {
                maxId = issue.getIssueId();
            }
        }
        return maxId + 1;
    }

    private Issue getProjectIssueOrNull(Project project, long issueId) {
        validateProject(project);
        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be a positive number.");
        }

        return issueRepository.findByProjectIdAndIssueId(project.getProjectId(), issueId);
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

    private void validateProjectMember(Project project, User user, String message) {
        if (project == null || user == null) {
            throw new IllegalArgumentException(message);
        }

        if (!project.getMembers().contains(user)) {
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

    // 이슈 삭제
    public void deleteIssue(Project project, long issueId, User user) {
        
        // 1. 권한 검증 (예: 프로젝트 멤버만 삭제 가능하다고 가정)
        validateMember(project, user);
        validateIssueId(issueId);

        // 2. Project 객체가 가진 이슈 리스트에서 해당 이슈 제거
        project.removeIssueById(issueId);

        // 3. IssueRepository를 통해 이슈 데이터 완전히 삭제 (파일에서 지워짐)
        issueRepository.delete(project.getProjectId(), issueId);
        updateProjectIfAvailable(project);

    }

    private void updateProjectIfAvailable(Project project) {
        if (projectRepository != null) {
            projectRepository.update(project);
        }
    }

    //이거 UI구현되면 수정필요
    public void showIssues(Project project, Status filterStatus) {
        validateProject(project);

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

    private void validateMember(Project project, User user) {
        validateProject(project);
        if (user == null || !project.getMembers().contains(user)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아니므로 권한이 없습니다.");
        }
    }

    private void validateIssueId(long issueId) {
        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be a positive number.");
        }
    }

    private void validateIssueStatus(Issue issue, Status... validStatuses) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found.");
        }
        if (!Arrays.asList(validStatuses).contains(issue.getStatus())) {
            throw new IllegalStateException(
                "이슈 상태가 올바르지 않습니다. 현재 상태: " + issue.getStatus()
            );
        }
    }

}
