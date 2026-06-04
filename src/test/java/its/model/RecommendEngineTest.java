package its.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendEngineTest {

    private RecommendEngine recommendEngine;

    private User dev1;
    private User dev2;
    private User dev3;
    private User tester;

    private Issue targetIssue;
    private Issue similar1Dev1;
    private Issue similar2Dev1;
    private Issue different1Dev1;
    private Issue similar3Dev2;
    private Issue different2Dev2;
    private Issue different3Dev3;

    private Category targetCategory;

    private List<Issue> issues;
    private List<User> developers;

    @BeforeEach
    void setUp() {
        recommendEngine = new RecommendEngine();

        dev1 = createUser(1L, "dev1", UserRole.DEVELOPER);
        dev2 = createUser(2L, "dev2", UserRole.DEVELOPER);
        dev3 = createUser(3L, "dev3", UserRole.DEVELOPER);
        tester = createUser(4L, "tester1", UserRole.TESTER);

        targetIssue = createIssue(
                1L,
                1,
                1,
                "login error",
                "login failed",
                IssueStatus.NEW,
                null,
                null
        );

        similar1Dev1 = createIssue(
                2L,
                1,
                1,
                "login error",
                "login failed",
                IssueStatus.RESOLVED,
                null,
                dev1
        );

        similar2Dev1 = createIssue(
                3L,
                1,
                1,
                "login error",
                "login failed",
                IssueStatus.CLOSED,
                null,
                dev1
        );

        different1Dev1 = createIssue(
                4L,
                1,
                2,
                "DB issue",
                "DB failed",
                IssueStatus.ASSIGNED,
                dev1,
                null
        );

        similar3Dev2 = createIssue(
                5L,
                1,
                1,
                "login error",
                "login failed",
                IssueStatus.ASSIGNED,
                dev2,
                dev2
        );

        different2Dev2 = createIssue(
                6L,
                1,
                2,
                "DB issue",
                "DB failed",
                IssueStatus.RESOLVED,
                null,
                dev2
        );

        different3Dev3 = createIssue(
                7L,
                1,
                3,
                "payment error",
                "payment failed",
                IssueStatus.RESOLVED,
                null,
                dev3
        );

        issues = new ArrayList<>();
        issues.add(targetIssue);
        issues.add(similar1Dev1);
        issues.add(similar2Dev1);
        issues.add(different1Dev1);
        issues.add(similar3Dev2);
        issues.add(different2Dev2);
        issues.add(different3Dev3);

        developers = new ArrayList<>();
        developers.add(dev1);
        developers.add(dev2);
        developers.add(dev3);
        developers.add(tester);

        targetCategory = createCategory();
    }

    private User createUser(long userId, String loginId, UserRole role) {
        return new User(
                userId,
                loginId,
                "pw",
                AccountStatus.ACTIVE,
                role
        );
    }

    private Issue createIssue(
            long issueId,
            int projectId,
            int categoryId,
            String title,
            String description,
            IssueStatus status,
            User assignee,
            User fixer
    ) {
        Issue issue = new Issue(
                issueId,
                projectId,
                title,
                description,
                tester,
                LocalDateTime.of(2026, 6, 3, 10, 0)
        );

        issue.setCategoryId(categoryId);
        issue.setStatus(status);

        if (assignee != null) {
            issue.setAssignee(assignee);
        }

        if (fixer != null) {
            issue.setFixer(fixer);
        }

        return issue;
    }

    private Category createCategory() {
        Map<String, Double> representVector = new HashMap<>();
        representVector.put("login", 1.0);
        representVector.put("error", 1.0);
        representVector.put("failed", 1.0);

        Map<String, Double> idfVector = new HashMap<>();
        idfVector.put("login", 1.0);
        idfVector.put("error", 1.0);
        idfVector.put("failed", 1.0);

        Category category = new Category(
                1,
                1,
                0.2,
                issues,
                representVector,
                idfVector
        );

        category.setCategoryName("Login");
        return category;
    }

    @Test
    void recommendDevelopers_invalidInput() {
        assertTrue(recommendEngine.recommendDevelopers(null, issues, developers, targetCategory, 3).isEmpty());

        assertTrue(recommendEngine.recommendDevelopers(targetIssue, null, developers, targetCategory, 3).isEmpty());

        assertTrue(recommendEngine.recommendDevelopers(targetIssue, issues, null, targetCategory, 3).isEmpty());

        assertTrue(recommendEngine.recommendDevelopers(targetIssue, issues, developers, targetCategory, 0).isEmpty());

        assertTrue(recommendEngine.recommendDevelopers(targetIssue, issues, developers, null, 3).isEmpty());
    }

    @Test
    void recommendDevelopers_topK() {
        List<DeveloperRecommendation> result =
                recommendEngine.recommendDevelopers(
                        targetIssue,
                        issues,
                        developers,
                        targetCategory,
                        2
                );

        assertEquals(2, result.size());
    }

    @Test
    void recommendDevelopers_defaultTopK() {
        List<DeveloperRecommendation> result =
                recommendEngine.recommendDevelopers(
                        targetIssue,
                        issues,
                        developers,
                        targetCategory
                );

        assertEquals(3, result.size());
    }

    @Test
    void recommendDevelopers_score() {
        List<DeveloperRecommendation> result =
                recommendEngine.recommendDevelopers(
                        targetIssue,
                        issues,
                        developers,
                        targetCategory,
                        3
                );

        DeveloperRecommendation dev1Recommendation = findByDeveloperId(result, dev1.getUserId());
        DeveloperRecommendation dev2Recommendation = findByDeveloperId(result, dev2.getUserId());
        DeveloperRecommendation dev3Recommendation = findByDeveloperId(result, dev3.getUserId());

        assertNotNull(dev1Recommendation);
        assertNotNull(dev2Recommendation);
        assertNotNull(dev3Recommendation);

        assertTrue(dev1Recommendation.getScore() > dev2Recommendation.getScore());
        assertTrue(dev1Recommendation.getScore() > dev3Recommendation.getScore());

        assertEquals(dev1.getUserId(), result.get(0).getDeveloper().getUserId());
    }

    @Test
    void recommendDevelopers_countsMatched() {
        List<DeveloperRecommendation> result =
                recommendEngine.recommendDevelopers(
                        targetIssue,
                        issues,
                        developers,
                        targetCategory,
                        3
                );

        DeveloperRecommendation dev1Recommendation = findByDeveloperId(result, dev1.getUserId());
        DeveloperRecommendation dev2Recommendation = findByDeveloperId(result, dev2.getUserId());
        DeveloperRecommendation dev3Recommendation = findByDeveloperId(result, dev3.getUserId());

        assertNotNull(dev1Recommendation);
        assertNotNull(dev2Recommendation);
        assertNotNull(dev3Recommendation);

        assertEquals(2, dev1Recommendation.getMatchedIssueCount());
        assertEquals(0, dev2Recommendation.getMatchedIssueCount());
        assertEquals(0, dev3Recommendation.getMatchedIssueCount());
    }

    @Test
    void recommendDevelopers_workloadPenalty() {
        List<DeveloperRecommendation> result =
                recommendEngine.recommendDevelopers(
                        targetIssue,    
                        issues,
                        developers,
                        targetCategory,
                        3
                );

        DeveloperRecommendation dev1Recommendation = findByDeveloperId(result, dev1.getUserId());
        DeveloperRecommendation dev2Recommendation = findByDeveloperId(result, dev2.getUserId());

        assertNotNull(dev1Recommendation);
        assertNotNull(dev2Recommendation);
        
        assertEquals(1.8, dev1Recommendation.getScore(), 0.000001);
        assertEquals(0.0, dev2Recommendation.getScore(), 0.000001);
    }

    private DeveloperRecommendation findByDeveloperId(
            List<DeveloperRecommendation> recommendations,
            long developerId
    ) {
        for (DeveloperRecommendation recommendation : recommendations) {
            if (recommendation.getDeveloper().getUserId() == developerId) {
                return recommendation;
            }
        }

        return null;
    }
}