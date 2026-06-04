package its.model;

import its.repository.CategoryRepository;
import its.repository.MemoryCategoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CategoryEngineTest {

    private CategoryEngine categoryEngine;
    private CategoryRepository categoryRepository;

    private Issue loginIssue1;
    private Issue loginIssue2;
    private Issue dbIssue;
    private Issue paymentIssue;

    private List<Issue> issues;

    @BeforeEach
    void setUp() {
        categoryRepository = new MemoryCategoryRepository();
        categoryEngine = new CategoryEngine(categoryRepository);

        loginIssue1 = createIssue(
                1L,
                1,
                0,
                "login error",
                "login failed",
                List.of("login error")
        );

        loginIssue2 = createIssue(
                2L,
                1,
                0,
                "login error",
                "login failed",
                List.of("login error")
        );

        dbIssue = createIssue(
                3L,
                1,
                0,
                "database error",
                "database failed",
                List.of("database error")
        );

        paymentIssue = createIssue(
                4L,
                1,
                0,
                "payment error",
                "payment failed",
                List.of("payment error")
        );

        issues = new ArrayList<>();
        issues.add(loginIssue1);
        issues.add(loginIssue2);
        issues.add(dbIssue);
        issues.add(paymentIssue);
    }

    private Issue createIssue(
            long issueId,
            int projectId,
            int categoryId,
            String title,
            String description,
            List<String> commentContents
    ) {
        Issue issue = new Issue(
                issueId,
                projectId,
                title,
                description,
                null,
                LocalDateTime.of(2026, 6, 3, 10, 0)
        );

        issue.setCategoryId(categoryId);

        int commentId = 1;
        for (String content : commentContents) {
            Comment comment = new Comment(
                    commentId++,
                    content,
                    null,
                    LocalDateTime.of(2026, 6, 3, 10, 0)
            );
            issue.addComment(comment);
        }

        return issue;
    }

    @Test
    void createCategoriesByThreshold_invalidInput() {
        assertTrue(categoryEngine.createCategoriesByThreshold(null, 0.25).isEmpty());
        assertTrue(categoryEngine.createCategoriesByThreshold(new ArrayList<>(), 0.25).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> {
            categoryEngine.createCategoriesByThreshold(issues, -0.1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            categoryEngine.createCategoriesByThreshold(issues, 1.1);
        });
    }

    @Test
    void createCategoriesByThreshold_() {
        List<Category> categories = categoryEngine.createCategoriesByThreshold(issues, 0.9);

        assertFalse(categories.isEmpty());

        for (Issue issue : issues) {
            assertTrue(issue.getCategoryId() > 0);
        }

        assertEquals(1, loginIssue1.getCategoryId());
        assertEquals(1, loginIssue2.getCategoryId());

        Category loginCategory = findCategoryById(categories, 1);
        assertNotNull(loginCategory);
        assertEquals(2, loginCategory.getIssues().size());
        assertFalse(loginCategory.getRepresentVector().isEmpty());
        assertFalse(loginCategory.getIdf().isEmpty());
    }

    @Test
    void categorizeSingleIssue_() {
        List<Category> categories = categoryEngine.createCategoriesByThreshold(issues, 0.25);

        Issue newLoginIssue = createIssue(
                100L,
                1,
                0,
                "login error",
                "login failed",
                List.of("login error")
        );

        int categoryId = categoryEngine.categorizeSingleIssue(newLoginIssue, categories);

        assertEquals(loginIssue1.getCategoryId(), categoryId);
    }

    @Test
    void categorizeSingleIssue_shouldReturnZero_whenNoSavedCategories() {
        Issue newIssue = createIssue(
                100L,
                1,
                0,
                "login error",
                "login failed",
                List.of("login auth error")
        );

        int categoryId = categoryEngine.categorizeSingleIssue(newIssue, new ArrayList<>());

        assertEquals(0, categoryId);
    }

    @Test
    void categorizeSingleIssue_shouldReturnZero_whenIssueIsNull() {
        List<Category> categories = categoryEngine.createCategoriesByThreshold(issues, 0.25);

        int categoryId = categoryEngine.categorizeSingleIssue(null, categories);

        assertEquals(0, categoryId);
    }

    @Test
    void mergeCategories_() {
        List<Category> categories = categoryEngine.createCategoriesByThreshold(issues, 0.9);
        Map<Long, Map<String, Double>> tfIdfVectors = categoryEngine.getTfIdf().calculateTfIdfByIssues(issues);

        Category categoryA = findCategoryById(categories, loginIssue1.getCategoryId());
        Category categoryB = findCategoryById(categories, dbIssue.getCategoryId());

        assertNotNull(categoryA);
        assertNotNull(categoryB);
        assertNotEquals(categoryA.getCategoryId(), categoryB.getCategoryId());

        Category mergedCategory = categoryEngine.mergeCategories(
                categoryA.getCategoryId(),
                categoryB.getCategoryId(),
                categories,
                tfIdfVectors
        );

        assertNotNull(mergedCategory);
        assertEquals(categoryA.getCategoryId(), mergedCategory.getCategoryId());
        assertEquals(categoryA.getIssues().size() + categoryB.getIssues().size(), mergedCategory.getIssues().size());

        for (Issue issue : mergedCategory.getIssues()) {
            assertEquals(categoryA.getCategoryId(), issue.getCategoryId());
        }

        assertFalse(mergedCategory.getRepresentVector().isEmpty());
    }

    @Test
    void partitionCategoryA_() {
        List<Category> categories = categoryEngine.createCategoriesByThreshold(issues, 0.25);
        Map<Long, Map<String, Double>> tfIdfVectors =
                categoryEngine.getTfIdf().calculateTfIdfByIssues(issues);

        int targetCategoryId = loginIssue1.getCategoryId();

        List<Issue> remainingIssues = new ArrayList<>();
        remainingIssues.add(loginIssue1);

        List<Issue> separatingIssues = new ArrayList<>();
        separatingIssues.add(loginIssue2);

        List<Category> result = categoryEngine.partitionCategoryA(
                targetCategoryId,
                remainingIssues,
                separatingIssues,
                categories,
                tfIdfVectors
        );

        assertEquals(2, result.size());

        Category updatedOriginalCategory = result.get(0);
        Category newCategory = result.get(1);

        assertEquals(targetCategoryId, updatedOriginalCategory.getCategoryId());
        assertEquals(1, updatedOriginalCategory.getIssues().size());
        assertEquals(updatedOriginalCategory.getCategoryId(), loginIssue1.getCategoryId());

        assertTrue(newCategory.getCategoryId() > targetCategoryId);
        assertEquals(1, newCategory.getIssues().size());
        assertEquals(newCategory.getCategoryId(), loginIssue2.getCategoryId());

        assertFalse(updatedOriginalCategory.getRepresentVector().isEmpty());
        assertFalse(newCategory.getRepresentVector().isEmpty());
    }

    @Test
    void resetCategory() {
        categoryEngine.createCategoriesByThreshold(issues, 0.25);

        assertFalse(categoryEngine.getVocabulary().isEmpty());

        categoryEngine.resetCategory(issues);

        for (Issue issue : issues) {
            assertEquals(0, issue.getCategoryId());
        }

        assertTrue(categoryEngine.getVocabulary().isEmpty());
    }

    private Category findCategoryById(List<Category> categories, int categoryId) {
        for (Category category : categories) {
            if (category != null && category.getCategoryId() == categoryId) {
                return category;
            }
        }

        return null;
    }
}