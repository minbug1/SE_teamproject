package its.controller;

import its.model.*;
import its.repository.IssueRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsControllerTest {

    // ?? ?몃찓紐⑤━ IssueRepository ?ㅽ뀅 ????????????????????????????????????????
    static class MemoryIssueRepository implements IssueRepository {
        private final List<Issue> issues = new ArrayList<>();

        @Override public void save(Issue issue) { issues.add(issue); }
        @Override public Issue findById(long issueId) {
            return issues.stream().filter(i -> i.getIssueId() == issueId).findFirst().orElse(null);
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
                if (issues.get(i).getIssueId() == issue.getIssueId()) { issues.set(i, issue); return; }
            }
        }
        @Override public void delete(long issueId) { issues.removeIf(i -> i.getIssueId() == issueId); }
        @Override public void delete(int projectId, long issueId) {
            issues.removeIf(i -> i.getProjectId() == projectId && i.getIssueId() == issueId);
        }
        @Override public long generateIssueId() {
            return issues.stream().mapToInt(i -> (int) i.getIssueId()).max().orElse(0) + 1;
        }
        @Override public long generateIssueId(int projectId) { return generateIssueId(); }
    }

    // ?????????????????????????????????????????????????????????????????????????

    private MemoryIssueRepository issueRepository;
    private StatisticsController statisticsController;

    private User reporter;
    private User dev1;
    private User dev2;

    private static final int PROJECT_ID = 1;

    @BeforeEach
    void setUp() {
        issueRepository = new MemoryIssueRepository();
        statisticsController = new StatisticsController(issueRepository);

        reporter = new User(1L, "reporter", "pw", AccountStatus.ACTIVE, UserRole.TESTER);
        dev1     = new User(2L, "dev1",     "pw", AccountStatus.ACTIVE, UserRole.DEVELOPER);
        dev2     = new User(3L, "dev2",     "pw", AccountStatus.ACTIVE, UserRole.DEVELOPER);
    }

    // ?? ?ы띁: ?댁뒋 ?앹꽦 ???????????????????????????????????????????????????????

    private Issue makeIssue(int id, LocalDateTime date, Priority priority, IssueStatus status, User fixer) {
        Issue issue = new Issue((long) id, PROJECT_ID, "Title" + id, "Desc", reporter, date);
        issue.setProjectId(PROJECT_ID);
        issue.setPriority(priority);
        issue.setStatus(status);
        if (fixer != null) issue.setFixer(fixer);
        issueRepository.save(issue);
        return issue;
    }

    // ?? getIssueCountByDay ????????????????????????????????????????????????????

    @Test
    void getIssueCountByDayShouldReturnZeroWhenNoIssues() {
        Map<Integer, Long> result = statisticsController.getIssueCountByDay(PROJECT_ID, 2026, 5);

        assertEquals(31, result.size()); // 5?붿? 31??
        result.values().forEach(count -> assertEquals(0L, count));
    }

    @Test
    void getIssueCountByDayShouldCountCorrectly() {
        makeIssue(1, LocalDateTime.of(2026, 5, 10, 9, 0), Priority.MAJOR, IssueStatus.NEW, null);
        makeIssue(2, LocalDateTime.of(2026, 5, 10, 15, 0), Priority.MINOR, IssueStatus.NEW, null);
        makeIssue(3, LocalDateTime.of(2026, 5, 20, 9, 0), Priority.BLOCKER, IssueStatus.NEW, null);

        Map<Integer, Long> result = statisticsController.getIssueCountByDay(PROJECT_ID, 2026, 5);

        assertEquals(2L, result.get(10));
        assertEquals(1L, result.get(20));
        assertEquals(0L, result.get(15));
    }

    @Test
    void getIssueCountByDayShouldIgnoreOtherMonths() {
        makeIssue(1, LocalDateTime.of(2026, 4, 10, 9, 0), Priority.MAJOR, IssueStatus.NEW, null); // 4??
        makeIssue(2, LocalDateTime.of(2026, 5, 10, 9, 0), Priority.MAJOR, IssueStatus.NEW, null); // 5??

        Map<Integer, Long> result = statisticsController.getIssueCountByDay(PROJECT_ID, 2026, 5);

        assertEquals(1L, result.get(10)); // 5??寃껊쭔 移댁슫??
    }

    @Test
    void getIssueCountByDayShouldIgnoreOtherProjects() {
        Issue issue = new Issue(1L, 999, "Title", "Desc", reporter, LocalDateTime.of(2026, 5, 10, 9, 0)); // ?ㅻⅨ ?꾨줈?앺듃
        issue.setPriority(Priority.MAJOR);
        issue.setStatus(IssueStatus.NEW);
        issueRepository.save(issue);

        Map<Integer, Long> result = statisticsController.getIssueCountByDay(PROJECT_ID, 2026, 5);

        assertEquals(0L, result.get(10));
    }

    @Test
    void getIssueCountByDayShouldFailWhenProjectIdIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                statisticsController.getIssueCountByDay(0, 2026, 5));
    }

    // ?? getIssueCountByMonth ??????????????????????????????????????????????????

    @Test
    void getIssueCountByMonthShouldReturnAllTwelveMonths() {
        Map<Integer, Long> result = statisticsController.getIssueCountByMonth(PROJECT_ID, 2026);
        assertEquals(12, result.size());
    }

    @Test
    void getIssueCountByMonthShouldCountCorrectly() {
        makeIssue(1, LocalDateTime.of(2026, 1, 5, 9, 0), Priority.MAJOR, IssueStatus.NEW, null);
        makeIssue(2, LocalDateTime.of(2026, 1, 20, 9, 0), Priority.MINOR, IssueStatus.NEW, null);
        makeIssue(3, LocalDateTime.of(2026, 3, 15, 9, 0), Priority.BLOCKER, IssueStatus.NEW, null);

        Map<Integer, Long> result = statisticsController.getIssueCountByMonth(PROJECT_ID, 2026);

        assertEquals(2L, result.get(1));
        assertEquals(1L, result.get(3));
        assertEquals(0L, result.get(6));
    }

    @Test
    void getIssueCountByMonthShouldIgnoreOtherYears() {
        makeIssue(1, LocalDateTime.of(2025, 5, 10, 9, 0), Priority.MAJOR, IssueStatus.NEW, null); // 2025
        makeIssue(2, LocalDateTime.of(2026, 5, 10, 9, 0), Priority.MAJOR, IssueStatus.NEW, null); // 2026

        Map<Integer, Long> result = statisticsController.getIssueCountByMonth(PROJECT_ID, 2026);

        assertEquals(1L, result.get(5));
    }

    @Test
    void getIssueCountByMonthShouldFailWhenProjectIdIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                statisticsController.getIssueCountByMonth(-1, 2026));
    }

    // ?? getIssueCountByPriority ???????????????????????????????????????????????

    @Test
    void getIssueCountByPriorityShouldReturnAllPriorities() {
        Map<Priority, Long> result = statisticsController.getIssueCountByPriority(PROJECT_ID);
        assertEquals(Priority.values().length, result.size());
    }

    @Test
    void getIssueCountByPriorityShouldCountCorrectly() {
        makeIssue(1, LocalDateTime.now(), Priority.BLOCKER, IssueStatus.NEW, null);
        makeIssue(2, LocalDateTime.now(), Priority.BLOCKER, IssueStatus.NEW, null);
        makeIssue(3, LocalDateTime.now(), Priority.MAJOR,   IssueStatus.NEW, null);
        makeIssue(4, LocalDateTime.now(), Priority.TRIVIAL, IssueStatus.NEW, null);

        Map<Priority, Long> result = statisticsController.getIssueCountByPriority(PROJECT_ID);

        assertEquals(2L, result.get(Priority.BLOCKER));
        assertEquals(1L, result.get(Priority.MAJOR));
        assertEquals(1L, result.get(Priority.TRIVIAL));
        assertEquals(0L, result.get(Priority.MINOR));
        assertEquals(0L, result.get(Priority.CRITICAL));
    }

    @Test
    void getIssueCountByPriorityShouldReturnZerosWhenNoIssues() {
        Map<Priority, Long> result = statisticsController.getIssueCountByPriority(PROJECT_ID);
        result.values().forEach(count -> assertEquals(0L, count));
    }

    @Test
    void getIssueCountByPriorityShouldFailWhenProjectIdIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                statisticsController.getIssueCountByPriority(0));
    }

    // ?? getResolvedCountByDeveloper ???????????????????????????????????????????

    @Test
    void getResolvedCountByDeveloperShouldReturnEmptyWhenNoResolvedIssues() {
        makeIssue(1, LocalDateTime.now(), Priority.MAJOR, IssueStatus.NEW, null);

        Map<User, Long> result = statisticsController.getResolvedCountByDeveloper(PROJECT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getResolvedCountByDeveloperShouldCountFixedResolvedClosed() {
        makeIssue(1, LocalDateTime.now(), Priority.MAJOR, IssueStatus.FIXED,    dev1);
        makeIssue(2, LocalDateTime.now(), Priority.MAJOR, IssueStatus.RESOLVED, dev1);
        makeIssue(3, LocalDateTime.now(), Priority.MAJOR, IssueStatus.CLOSED,   dev1);
        makeIssue(4, LocalDateTime.now(), Priority.MAJOR, IssueStatus.FIXED,    dev2);
        makeIssue(5, LocalDateTime.now(), Priority.MAJOR, IssueStatus.NEW,      dev1); // 移댁슫???쒖쇅

        Map<User, Long> result = statisticsController.getResolvedCountByDeveloper(PROJECT_ID);

        assertEquals(3L, result.get(dev1));
        assertEquals(1L, result.get(dev2));
    }

    @Test
    void getResolvedCountByDeveloperShouldIgnoreIssuesWithNoFixer() {
        makeIssue(1, LocalDateTime.now(), Priority.MAJOR, IssueStatus.RESOLVED, null); // fixer ?놁쓬

        Map<User, Long> result = statisticsController.getResolvedCountByDeveloper(PROJECT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getResolvedCountByDeveloperShouldFailWhenProjectIdIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                statisticsController.getResolvedCountByDeveloper(0));
    }

    // ?? getAvailableYears ?????????????????????????????????????????????????????

    @Test
    void getAvailableYearsShouldReturnCurrentYearWhenNoIssues() {
        List<Integer> years = statisticsController.getAvailableYears(PROJECT_ID);

        assertEquals(1, years.size());
        assertEquals(LocalDateTime.now().getYear(), years.get(0));
    }

    @Test
    void getAvailableYearsShouldReturnSortedUniqueYears() {
        makeIssue(1, LocalDateTime.of(2024, 3, 1, 9, 0), Priority.MAJOR, IssueStatus.NEW, null);
        makeIssue(2, LocalDateTime.of(2026, 5, 1, 9, 0), Priority.MAJOR, IssueStatus.NEW, null);
        makeIssue(3, LocalDateTime.of(2026, 8, 1, 9, 0), Priority.MAJOR, IssueStatus.NEW, null); // 2026 以묐났

        List<Integer> years = statisticsController.getAvailableYears(PROJECT_ID);

        assertEquals(List.of(2024, 2026), years);
    }

    @Test
    void getAvailableYearsShouldFailWhenProjectIdIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                statisticsController.getAvailableYears(-5));
    }

    // ?? ?앹꽦??寃利?????????????????????????????????????????????????????????????

    @Test
    void constructorShouldFailWhenRepositoryIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new StatisticsController(null));
    }
}
