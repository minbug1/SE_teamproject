package its.controller;

import its.model.*;
import its.repository.IssueRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IssueControllerTest {

    // ── 인메모리 IssueRepository 스텁 ────────────────────────────────────────
    static class MemoryIssueRepository implements IssueRepository {

        private final List<Issue> issues = new ArrayList<>();

        @Override public void save(Issue issue) { issues.add(issue); }

        @Override public Issue findById(long issueId) {
            return issues.stream()
                    .filter(i -> i.getIssueId() == issueId)
                    .findFirst().orElse(null);
        }

        @Override public Issue findByProjectIdAndIssueId(int projectId, long issueId) {
            return issues.stream()
                    .filter(i -> i.getProjectId() == projectId && i.getIssueId() == issueId)
                    .findFirst().orElse(null);
        }

        @Override public List<Issue> findAll() { return new ArrayList<>(issues); }

        @Override public List<Issue> findByProjectId(int projectId) {
            List<Issue> result = new ArrayList<>();
            for (Issue i : issues) if (i.getProjectId() == projectId) result.add(i);
            return result;
        }

        @Override public void update(Issue issue) {
            for (int i = 0; i < issues.size(); i++) {
                if (issues.get(i).getIssueId() == issue.getIssueId()
                        && issues.get(i).getProjectId() == issue.getProjectId()) {
                    issues.set(i, issue); return;
                }
            }
            throw new IllegalArgumentException("Issue does not exist.");
        }

        @Override public void delete(long issueId) {
            issues.removeIf(i -> i.getIssueId() == issueId);
        }

        @Override public void delete(int projectId, long issueId) {
            issues.removeIf(i -> i.getProjectId() == projectId && i.getIssueId() == issueId);
        }

        @Override public long generateIssueId() {
            return issues.stream().mapToInt(i -> (int) i.getIssueId()).max().orElse(0) + 1;
        }

        @Override public long generateIssueId(int projectId) {
            return issues.stream()
                    .filter(i -> i.getProjectId() == projectId)
                    .mapToInt(i -> (int) i.getIssueId()).max().orElse(0) + 1;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private MemoryIssueRepository issueRepository;
    private IssueController issueController;

    private Project project;
    private User pl;
    private User dev;
    private User tester;
    private User outsider;

    @BeforeEach
    void setUp() {
        issueRepository = new MemoryIssueRepository();
        issueController = new IssueController(issueRepository);

        pl      = new User(1L, "pl",      "pw", AccountStatus.ACTIVE, Role.PL);
        dev     = new User(2L, "dev",     "pw", AccountStatus.ACTIVE, Role.DEVELOPER);
        tester  = new User(3L, "tester",  "pw", AccountStatus.ACTIVE, Role.TESTER);
        outsider= new User(4L, "outside", "pw", AccountStatus.ACTIVE, Role.DEVELOPER);

        project = new Project(1, "TestProject", "desc");
        project.addMember(pl);
        project.addMember(dev);
        project.addMember(tester);
    }

    // ── reportIssue ───────────────────────────────────────────────────────────

    @Test
    void reportIssueShouldSucceedForProjectMember() {
        Issue issue = issueController.reportIssue(
                project, "Bug title", "Bug desc", tester, Priority.MAJOR, "");

        assertNotNull(issue);
        assertEquals("Bug title", issue.getTitle());
        assertEquals(Status.NEW, issue.getStatus());
        assertEquals(Priority.MAJOR, issue.getPriority());
        assertEquals(tester, issue.getReporter());
        assertEquals(project.getProjectId(), issue.getProjectId());
        assertTrue(issue.getIssueId() > 0);
    }

    @Test
    void reportIssueShouldAttachCommentWhenProvided() {
        Issue issue = issueController.reportIssue(
                project, "Title", "Desc", tester, Priority.MINOR, "First comment");

        assertEquals(1, issue.getComments().size());
        assertEquals("First comment", issue.getComments().get(0).getContent());
    }

    @Test
    void reportIssueShouldNotAttachCommentWhenEmpty() {
        Issue issue = issueController.reportIssue(
                project, "Title", "Desc", tester, Priority.MINOR, "   ");

        assertTrue(issue.getComments().isEmpty());
    }

    @Test
    void reportIssueShouldFailWhenReporterIsNotMember() {
        assertThrows(SecurityException.class, () ->
                issueController.reportIssue(
                        project, "Title", "Desc", outsider, Priority.MAJOR, ""));
    }

    @Test
    void reportIssueShouldFailWhenTitleIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                issueController.reportIssue(
                        project, "  ", "Desc", tester, Priority.MAJOR, ""));
    }

    @Test
    void reportIssueShouldFailWhenDescriptionIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                issueController.reportIssue(
                        project, "Title", "   ", tester, Priority.MAJOR, ""));
    }

    @Test
    void reportIssueShouldFailWhenProjectIsNull() {
        assertThrows(Exception.class, () ->
                issueController.reportIssue(
                        null, "Title", "Desc", tester, Priority.MAJOR, ""));
    }

    // ── assignIssue ───────────────────────────────────────────────────────────

    @Test
    void assignIssueShouldSucceedWhenPlAssignsDev() {
        Issue issue = reportSampleIssue();

        Issue assigned = issueController.assignIssue(project, issue.getIssueId(), dev, pl, "Assigned");

        assertNotNull(assigned);
        assertEquals(Status.ASSIGNED, assigned.getStatus());
        assertEquals(dev, assigned.getAssignee());
    }

    @Test
    void assignIssueShouldFailWhenAssignerIsNotPL() {
        Issue issue = reportSampleIssue();
        assertThrows(SecurityException.class, () ->
                issueController.assignIssue(project, issue.getIssueId(), dev, dev, ""));
    }

    @Test
    void assignIssueShouldFailWhenAssigneeIsNotDeveloper() {
        Issue issue = reportSampleIssue();
        assertThrows(SecurityException.class, () ->
                issueController.assignIssue(project, issue.getIssueId(), tester, pl, ""));
    }

    @Test
    void assignIssueShouldReturnNullWhenStatusIsNotNewOrReopened() {
        Issue issue = reportSampleIssue();
        issueController.assignIssue(project, issue.getIssueId(), dev, pl, "");
        // 이미 ASSIGNED 상태에서 다시 시도
        Issue result = issueController.assignIssue(project, issue.getIssueId(), dev, pl, "");
        assertNull(result);
    }

    // ── fixIssue ──────────────────────────────────────────────────────────────

    @Test
    void fixIssueShouldSucceedWhenAssignedDevFixes() {
        Issue issue = reportAndAssign();

        Issue fixed = issueController.fixIssue(project, issue.getIssueId(), "Fixed it", dev);

        assertNotNull(fixed);
        assertEquals(Status.FIXED, fixed.getStatus());
        assertEquals(dev, fixed.getFixer());
    }

    @Test
    void fixIssueShouldFailWhenDifferentDevTriesToFix() {
        Issue issue = reportAndAssign();

        User anotherDev = new User(5L, "dev2", "pw", AccountStatus.ACTIVE, Role.DEVELOPER);
        project.addMember(anotherDev);

        assertThrows(SecurityException.class, () ->
                issueController.fixIssue(project, issue.getIssueId(), "", anotherDev));
    }

    @Test
    void fixIssueShouldThrowWhenStatusIsNotAssigned() {
        Issue issue = reportSampleIssue(); // Status.NEW

        assertThrows(IllegalStateException.class, () ->
            issueController.fixIssue(project, issue.getIssueId(), "", dev));
    }
    // ── verifyIssue ───────────────────────────────────────────────────────────

    @Test
    void verifyIssueShouldResolveWhenTesterApproves() {
        // tester가 reporter이어야 함
        Issue issue = reportSampleIssueBy(tester);
        assignAndFix(issue);

        Issue verified = issueController.verifyIssue(project, issue.getIssueId(), "Looks good", tester, true);

        assertNotNull(verified);
        assertEquals(Status.RESOLVED, verified.getStatus());
    }

    @Test
    void verifyIssueShouldReopenWhenTesterRejects() {
        Issue issue = reportSampleIssueBy(tester);
        assignAndFix(issue);

        Issue verified = issueController.verifyIssue(project, issue.getIssueId(), "Still broken", tester, false);

        assertNotNull(verified);
        assertEquals(Status.REOPENED, verified.getStatus());
    }

    @Test
    void verifyIssueShouldFailWhenVerifierIsNotTester() {
        Issue issue = reportSampleIssueBy(tester);
        assignAndFix(issue);

        assertThrows(SecurityException.class, () ->
                issueController.verifyIssue(project, issue.getIssueId(), "", pl, true));
    }

    @Test
    void verifyIssueShouldFailWhenVerifierIsNotReporter() {
        // pl이 reporter, tester가 verify 시도 → reporter가 아님
        Issue issue = reportSampleIssueBy(pl);
        assignAndFix(issue);

        assertThrows(SecurityException.class, () ->
                issueController.verifyIssue(project, issue.getIssueId(), "", tester, true));
    }

    @Test
    void verifyIssueShouldReturnNullWhenStatusIsNotFixed() {
        Issue issue = reportSampleIssueBy(tester); // Status.NEW
        Issue result = issueController.verifyIssue(project, issue.getIssueId(), "", tester, true);
        assertNull(result);
    }

    // ── closeIssue ────────────────────────────────────────────────────────────

    @Test
    void closeIssueShouldSucceedWhenPlClosesResolvedIssue() {
        Issue issue = reportSampleIssueBy(tester);
        assignAndFix(issue);
        issueController.verifyIssue(project, issue.getIssueId(), "", tester, true);

        Issue closed = issueController.closeIssue(project, issue.getIssueId(), "Closing", pl);

        assertNotNull(closed);
        assertEquals(Status.CLOSED, closed.getStatus());
    }

    @Test
    void closeIssueShouldFailWhenCloserIsNotPL() {
        Issue issue = reportSampleIssueBy(tester);
        assignAndFix(issue);
        issueController.verifyIssue(project, issue.getIssueId(), "", tester, true);

        assertThrows(SecurityException.class, () ->
                issueController.closeIssue(project, issue.getIssueId(), "", dev));
    }

    @Test
    void closeIssueShouldReturnNullWhenStatusIsNotResolved() {
        Issue issue = reportSampleIssue(); // Status.NEW
        Issue result = issueController.closeIssue(project, issue.getIssueId(), "", pl);
        assertNull(result);
    }

    // ── deleteIssue ───────────────────────────────────────────────────────────

    @Test
    void deleteIssueShouldRemoveIssueForMember() {
        Issue issue = reportSampleIssue();
        long id = issue.getIssueId();

        issueController.deleteIssue(project, id, tester);

        assertNull(issueRepository.findByProjectIdAndIssueId(project.getProjectId(), (int) id));
    }

    @Test
    void deleteIssueShouldFailForNonMember() {
        Issue issue = reportSampleIssue();
        assertThrows(SecurityException.class, () ->
                issueController.deleteIssue(project, issue.getIssueId(), outsider));
    }

    // ── addComment ────────────────────────────────────────────────────────────

    @Test
    void addCommentShouldSucceedForMember() {
        Issue issue = reportSampleIssue();

        issueController.addComment(project, issue.getIssueId(), "New comment", dev);

        Issue updated = issueRepository.findByProjectIdAndIssueId(
                project.getProjectId(), (int) issue.getIssueId());
        assertEquals(1, updated.getComments().size());
        assertEquals("New comment", updated.getComments().get(0).getContent());
    }

    @Test
    void addCommentShouldFailWhenCommentIsEmpty() {
        Issue issue = reportSampleIssue();
        assertThrows(IllegalArgumentException.class, () ->
                issueController.addComment(project, issue.getIssueId(), "  ", dev));
    }

    @Test
    void addCommentShouldFailForNonMember() {
        Issue issue = reportSampleIssue();
        assertThrows(SecurityException.class, () ->
                issueController.addComment(project, issue.getIssueId(), "comment", outsider));
    }

    // ── getAllIssues ───────────────────────────────────────────────────────────

    @Test
    void getAllIssuesShouldReturnAllIssues() {
        reportSampleIssue();
        reportSampleIssue();

        assertEquals(2, issueController.getAllIssues().size());
    }

    // ── 헬퍼 메서드 ───────────────────────────────────────────────────────────

    private Issue reportSampleIssue() {
        return reportSampleIssueBy(tester);
    }

    private Issue reportSampleIssueBy(User reporter) {
        return issueController.reportIssue(
                project, "Sample Issue", "Sample Description", reporter, Priority.MAJOR, "");
    }

    private Issue reportAndAssign() {
        Issue issue = reportSampleIssue();
        issueController.assignIssue(project, issue.getIssueId(), dev, pl, "");
        // repository에서 최신 상태 가져오기
        return issueRepository.findByProjectIdAndIssueId(project.getProjectId(), (int) issue.getIssueId());
    }

    private void assignAndFix(Issue issue) {
        issueController.assignIssue(project, issue.getIssueId(), dev, pl, "");
        Issue assigned = issueRepository.findByProjectIdAndIssueId(
                project.getProjectId(), (int) issue.getIssueId());
        issueController.fixIssue(project, assigned.getIssueId(), "", dev);
    }
}