package its.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TfIdfTest {

    private TfIdf tfIdf;

    private Issue issueWithOneComment;
    private Issue issueWithTwoComments;
    private Issue issueWithThreeComments;

    private List<Issue> issues;

    @BeforeEach
    void setUp() {
        tfIdf = new TfIdf();
        // login error login error login error login login can t login
        // login 6, error 3
        issueWithOneComment = createIssue(
                1L,
                1,
                1,
                "login error",
                "login",
                List.of("can't login")
        );

        // repository error repository error repository error repository repository I can t save I can read
        // repository 5, error 3
        issueWithTwoComments = createIssue(
                2L,
                1,
                2,
                "repository error",
                "repository",
                List.of(
                        "I can't save",
                        "I can read"
                )
        );
        // payment error payment error payment error payment payment I can t pay I can refund I can
        // payment 5, error 3
        issueWithThreeComments = createIssue(
                3L,
                1,
                3,
                "payment error",
                "payment",
                List.of(
                        "I can't pay",
                        "I can refund",
                        "I can"
                )
        );

        issues = new ArrayList<>();
        issues.add(issueWithOneComment);
        issues.add(issueWithTwoComments);
        issues.add(issueWithThreeComments);
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

    // tokenize, add, cut, countwords
    @Test
    void tokenize_add_cut_countwords() {
        Map<String, Integer> wordCounts = tfIdf.countWordsByIssue(issueWithOneComment);

        assertEquals(6, wordCounts.get("login"));
        assertEquals(3, wordCounts.get("error"));

        assertEquals(1, wordCounts.get("can"));
        assertEquals(1, wordCounts.get("t"));
    }

    @Test
    void calculateTfByIssue_oneComment() {
        Set<String> vocabulary = new HashSet<>();
        vocabulary.add("login");
        vocabulary.add("error");
        vocabulary.add("can");
        vocabulary.add("t");

        Map<String, Double> tfVector = tfIdf.calculateTfByIssue(issueWithOneComment, vocabulary);

        assertEquals(6.0 / 9.0, tfVector.get("login"), 0.000001);
        assertEquals(3.0 / 9.0, tfVector.get("error"), 0.000001);
        assertEquals(0.0 / 9.0, tfVector.get("can"), 0.000001);
        assertEquals(0.0 / 9.0, tfVector.get("t"), 0.000001);
    }

    @Test
    void calculateIdfByDocument() {
        tfIdf.buildVocabulary(issues);

        Map<String, Double> idf = tfIdf.calculateIdfByDocument(issues);

        double expectedIdfZero = Math.log((3.0 + 1.0) / (0.0 + 1.0)) + 1.0;
        double expectedIdfOne = Math.log((3.0 + 1.0) / (1.0 + 1.0)) + 1.0;

        assertEquals(expectedIdfOne, idf.get("login"), 0.000001);
        assertEquals(expectedIdfZero, idf.get("error"), 0.000001);
        assertEquals(expectedIdfOne, idf.get("can"), 0.000001);
        assertEquals(expectedIdfOne, idf.get("i"), 0.000001);
    }

    @Test
    void calculateTfIdfByIssue() {
        Set<String> vocabulary = new HashSet<>();
        vocabulary.add("login");
        vocabulary.add("error");
        vocabulary.add("can");
        vocabulary.add("t");

        Map<String, Double> idf = new HashMap<>();
        idf.put("login", 2.0);
        idf.put("error", 3.0);
        idf.put("can", 4.0);
        idf.put("t", 5.0);

        Map<String, Double> tfIdfVector =
                tfIdf.calculateTfIdfByIssue(issueWithOneComment, vocabulary, idf);

        assertEquals((6.0 / 9.0) * 2.0, tfIdfVector.get("login"), 0.000001);
        assertEquals((3.0 / 9.0) * 3.0, tfIdfVector.get("error"), 0.000001);
        assertEquals(0.0, tfIdfVector.get("can"), 0.000001);
        assertEquals(0.0, tfIdfVector.get("t"), 0.000001);
    }

    @Test
    void calculateTfIdfByIssues() {
        Map<Long, Map<String, Double>> result = tfIdf.calculateTfIdfByIssues(issues);

        assertEquals(3, result.size());
        assertTrue(result.containsKey(1L));
        assertTrue(result.containsKey(2L));
        assertTrue(result.containsKey(3L));

        Map<String, Double> issue1Vector = result.get(1L);

        double loginTf = 6.0 / 9.0;
        double loginIdf = Math.log((3.0 + 1.0) / (1.0 + 1.0)) + 1.0;

        assertEquals(loginTf * loginIdf, issue1Vector.get("login"), 0.000001);
    }

    @Test
    void cosineSimilarity_same() {
        tfIdf.buildVocabulary(issues);

        Map<String, Double> vectorA = new HashMap<>();
        vectorA.put("login", 1.0);
        vectorA.put("error", 1.0);

        Map<String, Double> vectorB = new HashMap<>();
        vectorB.put("login", 2.0);
        vectorB.put("error", 2.0);

        double similarity = tfIdf.cosineSimilarity(vectorA, vectorB);

        assertEquals(1.0, similarity, 0.000001);
    }

    @Test
    void cosineSimilarity_different() {
        tfIdf.buildVocabulary(issues);

        Map<String, Double> vectorA = new HashMap<>();
        vectorA.put("login", 1.0);

        Map<String, Double> vectorB = new HashMap<>();
        vectorB.put("repository", 1.0);

        double similarity = tfIdf.cosineSimilarity(vectorA, vectorB);

        assertEquals(0.0, similarity, 0.000001);
    }
}