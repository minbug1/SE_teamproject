package its.controller;

import its.model.Comment;
import its.model.Category;
import its.model.CategoryEngine;
import its.model.DeveloperRecommendation;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.RecommendEngine;
import its.model.IssueStatus;
import its.model.UserRole;
import its.model.User;
import its.repository.CategoryRepository;
import its.repository.FileIssueRepository;
import its.repository.IssueRepository;
import its.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IssueController {

    private IssueRepository issueRepository;
    private ProjectRepository projectRepository;
    private CategoryRepository categoryRepository;

    // 기본 생성자에서 FileIssueRepository를 사용하도록 설정
    public IssueController() {
        this(new FileIssueRepository());
    }

    // 의존성 주입을 위한 생성자
    public IssueController(IssueRepository issueRepository) {
        this(issueRepository, null);
    }

    public IssueController(IssueRepository issueRepository, ProjectRepository projectRepository) {
        this(issueRepository, projectRepository, null);
    }

    public IssueController(IssueRepository issueRepository, ProjectRepository projectRepository,
                           CategoryRepository categoryRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }

    public IssueRepository getIssueRepository() {
        return issueRepository;
    }

    //Report Issue
    public Issue reportIssue(Project project, String title, String description, User reporter, Priority priority, String commentContent) {

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
        newIssue.setStatus(IssueStatus.NEW);

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
        validateIssueStatus(issue, IssueStatus.ASSIGNED);

        if (issue.getAssignee() == null || !issue.getAssignee().equals(dev)) {
            throw new SecurityException("Only assigned issues can be fixed.");
        }

        addCommentIfPresent(issue, commentContent, dev);

        issue.setStatus(IssueStatus.FIXED);
        issue.setFixer(dev);
        issue.setFixedDate(LocalDateTime.now());

        issueRepository.update(issue);
        
        return issue;
    }

    //Verify Issue
    public Issue verifyIssue(Project project, long issueId, String commentContent, User tester, boolean isResolved){

        validateProject(project);
        validateUser(tester, "Tester must not be null.");
        validateRole(tester, UserRole.TESTER, "Only testers can verify issues.");
        validateProjectMember(project, tester, "Need to be a member of the project to verify the issue.");

        Issue issue = getProjectIssueOrNull(project, issueId);

        if (issue == null || issue.getStatus() != IssueStatus.FIXED) {
            return null;
        }

        if (issue.getReporter() == null || !issue.getReporter().equals(tester)) {
            throw new SecurityException("Only the reporter can verify the issue.");
        }

        if (isResolved) {
            issue.setStatus(IssueStatus.RESOLVED);
            issue.setResolvedDate(LocalDateTime.now());
        } else {
            issue.setStatus(IssueStatus.REOPENED);
            issue.incrementReopenCount();
        }

        addCommentIfPresent(issue, commentContent, tester);

        issueRepository.update(issue);
        
        return issue;
    }

    //Close Issue
    public Issue closeIssue(Project project, long issueId, String commentContent, User pl){

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateRole(pl, UserRole.PL, "Only PL can change the issue status to closed.");
        validateProjectMember(project, pl, "Need to be a member of the project to close the issue.");

        Issue issue = getProjectIssueOrNull(project, issueId);

        if (issue == null || issue.getStatus() != IssueStatus.RESOLVED) {
            return null;
        }

        addCommentIfPresent(issue, commentContent, pl);

        issue.setStatus(IssueStatus.CLOSED);
        issue.setClosedDate(LocalDateTime.now());
        
        issueRepository.update(issue);

        return issue;
    }

    //Assign Issue 이거 조금더 수정 필요함
    public Issue assignIssue(Project project, long issueId, User assignee, User pl, String commentContent) {

        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateUser(assignee, "Assignee must not be null.");

        validateRole(pl, UserRole.PL, "Only PL can assign issues.");
        validateRole(assignee, UserRole.DEVELOPER, "Only developers can be assigned to issues.");

        validateProjectMember(project, pl, "This user is not a PL member of the project.");
        validateProjectMember(project, assignee, "The assignee is not a member of the project.");

        Issue issue = getProjectIssueOrNull(project, issueId);

        if (issue == null || (issue.getStatus() != IssueStatus.NEW && issue.getStatus() != IssueStatus.REOPENED)) {
            return null;
        }

        issue.setAssignee(assignee);
        issue.setStatus(IssueStatus.ASSIGNED);
        issue.setAssignedDate(LocalDateTime.now());

        addCommentIfPresent(issue, commentContent, pl);

        issueRepository.update(issue);

        return issue;
    }

    // assign recommendation
    public List<DeveloperRecommendation> recommendAssignees(Project project, long issueId, User pl) {
        validateProject(project);
        validateUser(pl, "PL must not be null.");
        validateRole(pl, UserRole.PL, "Only PL can get recommendations.");
        validateProjectMember(project, pl, "This user is not a PL member of the project.");

        Issue targetIssue = getProjectIssueOrNull(project, issueId);

        if (targetIssue == null ||
            (targetIssue.getStatus() != IssueStatus.NEW &&
            targetIssue.getStatus() != IssueStatus.REOPENED)) {
            return new ArrayList<>();
        }

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        classifyTargetIssueIfNeeded(project, targetIssue, projectIssues);

        List<User> developers = new ArrayList<>();
        for (User member : project.getMembers()) {
            if (member != null && member.isDev()) {
                developers.add(member);
            }
        }

        RecommendEngine recommendEngine = new RecommendEngine(categoryRepository);
        return recommendEngine.recommendDevelopers(targetIssue, projectIssues, developers);
    }

    private void classifyTargetIssueIfNeeded(Project project, Issue targetIssue, List<Issue> projectIssues) {
        if (categoryRepository == null || targetIssue == null || targetIssue.getCategoryId() > 0) {
            return;
        }
        if (!hasCategorizedIssue(projectIssues)) {
            return;
        }

        List<Category> savedCategories = categoryRepository.findByProjectId(project.getProjectId());
        if (savedCategories == null || savedCategories.isEmpty()) {
            return;
        }

        CategoryEngine categoryEngine = new CategoryEngine(categoryRepository);
        int categoryId = categoryEngine.categorizeSingleIssue(targetIssue, savedCategories);
        if (categoryId <= 0) {
            return;
        }

        targetIssue.setCategoryId(categoryId);
        for (Issue issue : projectIssues) {
            if (issue != null && issue.getIssueId() == targetIssue.getIssueId()) {
                issue.setCategoryId(categoryId);
                break;
            }
        }

        issueRepository.update(targetIssue);
        addIssueToSavedCategory(savedCategories, categoryId, targetIssue);
        categoryRepository.saveAll(project.getProjectId(), savedCategories);
    }

    private void addIssueToSavedCategory(List<Category> categories, int categoryId, Issue issueToAdd) {
        for (Category category : categories) {
            if (category == null || category.getCategoryId() != categoryId) {
                continue;
            }

            boolean alreadyExists = category.getIssues().stream()
                    .anyMatch(issue -> issue != null && issue.getIssueId() == issueToAdd.getIssueId());
            if (!alreadyExists) {
                category.getIssues().add(issueToAdd);
            }
            return;
        }
    }

    private boolean hasCategorizedIssue(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return false;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getCategoryId() > 0) {
                return true;
            }
        }

        return false;
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

    public Issue addComment(Project project, long issueId, String commentContent, User author) {
        validateProject(project);
        validateUser(author, "Author must not be null.");
        validateMember(project, author);
        validateIssueId(issueId);

        if (commentContent == null || commentContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment must not be empty.");
        }

        Issue issue = getProjectIssueOrNull(project, issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found.");
        }

        addCommentIfPresent(issue, commentContent, author);
        issueRepository.update(issue);

        return issue;
    }

    public void changePriority(Project project, long issueId, Priority priority, User user) {
        validateProject(project);
        validateUser(user, "User must not be null.");
        validateMember(project, user);
        validateIssueId(issueId);

        if (priority == null) {
            throw new IllegalArgumentException("Priority must not be null.");
        }

        Issue issue = getProjectIssueOrNull(project, issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found.");
        }

        issue.setPriority(priority);
        issueRepository.update(issue);
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

   private void validateRole(User user, UserRole requiredRole, String message) {
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
    public void showIssues(Project project, IssueStatus filterStatus) {
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

    private void validateIssueStatus(Issue issue, IssueStatus... validStatuses) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found.");
        }
        if (!Arrays.asList(validStatuses).contains(issue.getStatus())) {
            throw new IllegalStateException(
                "이슈 상태가 올바르지 않습니다. 현재 상태: " + issue.getStatus()
            );
        }
    }

    public Issue getIssue(Project project, long issueId) {
        validateProject(project);
        validateIssueId(issueId);
        return getProjectIssueOrNull(project, issueId);
    }

}