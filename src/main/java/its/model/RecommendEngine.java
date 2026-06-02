package its.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * model for developer recommendation
 * 
 * @author hanung
 */

public class RecommendEngine {
    // top-k
    private static final int DEFAULT_TOP_N = 3;
    // workload penalty
    private static final double WORKLOAD_PENALTY_WEIGHT = 0.2;

    private final TfIdf tfIdf = new TfIdf();

    // constructor
    public RecommendEngine() {}

    // default top-k
    public List<DeveloperRecommendation> recommendDevelopers(Issue targetIssue, List<Issue> Issues, List<User> developers, Category targetCategory) {
        return recommendDevelopers(targetIssue, Issues, developers, targetCategory, DEFAULT_TOP_N);
    }

    // manual top-k
    public List<DeveloperRecommendation> recommendDevelopers(Issue targetIssue, List<Issue> issues, List<User> developers, Category targetCategory, int topK) {
        
        if (targetIssue == null || issues == null || developers == null || topK <= 0) {
            return new ArrayList<>();
        }

        int targetCategoryId = targetIssue.getCategoryId();
            if (targetCategoryId <= 0) {
                return new ArrayList<>();
            }

        if (targetCategory.getCategoryId() != targetCategoryId) {
            return new ArrayList<>();
        }

        Map<String, Double> storedIdf = targetCategory.getIdf();
        if (storedIdf == null || storedIdf.isEmpty()) {
            return new ArrayList<>();
        }
    
        // 같은 category 이슈 추출
        List<Issue> sameCategoryIssues = findIssuesByCategory(issues, targetIssue, targetCategoryId);
        if (sameCategoryIssues.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> vocabulary = new HashSet<>();

        if (targetCategory.getIdf() != null) {
            vocabulary.addAll(targetCategory.getIdf().keySet());
        }

        Map<String, Double> targetVector =
                tfIdf.calculateTfIdfByIssue(targetIssue, vocabulary, storedIdf);


        if (targetVector == null || targetVector.isEmpty()) {
            return buildZeroScoreRecommendations(developers, topK);
        }

        // dev data
        Map<Long, User> developerById = buildDeveloperMap(developers);
        Map<Long, Double> scoreByDeveloperId = new HashMap<>();
        Map<Long, Integer> matchedIssueCountByDeveloperId = new HashMap<>();
        Map<Long, Integer> resolvedIssueCountByDeveloperId = new HashMap<>();
        Map<Long, Integer> assignedCountByDeveloperId = new HashMap<>();

        for (User developer : developers) {
            if (developer == null || !developer.isDev()) {
                continue;
            }

            long id = developer.getUserId();
            scoreByDeveloperId.put(id, 0.0);
            matchedIssueCountByDeveloperId.put(id, 0);
            resolvedIssueCountByDeveloperId.put(id, 0);
            assignedCountByDeveloperId.put(id, 0);
        }

        countAssignedWorkload(issues, assignedCountByDeveloperId);

        for (Issue pastIssue : sameCategoryIssues) {
            if (pastIssue == null) {
                continue;
            }

            // valid check
            if (!isValidIssue(pastIssue, targetIssue, targetCategoryId)) {
                continue;
            }

            // find fixer
            User fixer = pastIssue.getFixer();
            if (fixer == null || !developerById.containsKey(fixer.getUserId())) {
                continue;
            }

            // resolvedIssueCount
            long fixerId = fixer.getUserId();
            resolvedIssueCountByDeveloperId.put(
                    fixerId,
                    resolvedIssueCountByDeveloperId.get(fixerId) + 1
            );

            // similarity
            Map<String, Double> pastVector =
                    tfIdf.calculateTfIdfByIssue(pastIssue, vocabulary, storedIdf);

            double similarity = tfIdf.cosineSimilarity(targetVector, pastVector);

            if (similarity <= 0.0) {
                continue;
            }

            // similarity score
            scoreByDeveloperId.put(
                    fixerId,
                    scoreByDeveloperId.get(fixerId) + similarity
            );

            matchedIssueCountByDeveloperId.put(
                    fixerId,
                    matchedIssueCountByDeveloperId.get(fixerId) + 1
            );
        }

        // recommend developer
        List<DeveloperRecommendation> recommendations = new ArrayList<>();
        for (User developer : developers) {
            if (developer == null || !developer.isDev()) {
                continue;
            }
            long id = developer.getUserId();

            double rawScore = scoreByDeveloperId.get(id);
            int assignedCount = assignedCountByDeveloperId.get(id);
            double finalScore = Math.max(0.0, rawScore - (assignedCount * WORKLOAD_PENALTY_WEIGHT));

            recommendations.add(new DeveloperRecommendation(
                    developer,
                    finalScore,
                    matchedIssueCountByDeveloperId.get(id),
                    resolvedIssueCountByDeveloperId.get(id)
            ));
        }

        Collections.sort(recommendations);

        List<DeveloperRecommendation> result = limit(recommendations, topK);

        return result;
    }

    // valid check
    private boolean isValidIssue(Issue pastIssue, Issue targetIssue, int targetCategoryId) {
        if (pastIssue == null || targetIssue == null) {
            return false;
        }

        // issue itself
        if (pastIssue.getIssueId() == targetIssue.getIssueId()) {
            return false;
        }

        // 같은 category 내부만
        if (pastIssue.getCategoryId() != targetCategoryId) {
            return false;
        }

        // no fixer
        if (pastIssue.getFixer() == null) {
            return false;
        }

        // resolved or closed
        boolean result = pastIssue.getStatus() == IssueStatus.RESOLVED || pastIssue.getStatus() == IssueStatus.CLOSED;

        return result;
    }

    // 같은 category 이슈 추출
    private List<Issue> findIssuesByCategory(List<Issue> issues, Issue targetIssue, int categoryId) {
        List<Issue> result = new ArrayList<>();

        if (issues == null || issues.isEmpty() || targetIssue == null || categoryId <= 0) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            // 자기 자신 제외
            if (issue.getIssueId() == targetIssue.getIssueId()) {
                continue;
            }

            if (issue.getCategoryId() == categoryId) {
                result.add(issue);
            }
        }

        return result;
    }

    private void countAssignedWorkload(List<Issue> issues, Map<Long, Integer> assignedCountByDeveloperId) {
        if (issues == null || assignedCountByDeveloperId == null) {
            return;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getStatus() == IssueStatus.ASSIGNED && issue.getAssignee() != null) {
                long assigneeId = issue.getAssignee().getUserId();

                if (assignedCountByDeveloperId.containsKey(assigneeId)) {
                    assignedCountByDeveloperId.put(
                            assigneeId,
                            assignedCountByDeveloperId.get(assigneeId) + 1
                    );
                }
            }
        }
    }

    // only dev
    private Map<Long, User> buildDeveloperMap(List<User> developers) {
        Map<Long, User> result = new HashMap<>();
        if (developers == null) return result;
        for (User dev : developers) {
            if (dev != null && dev.isDev()) {
                result.put(dev.getUserId(), dev);
            }
        }
        
        return result;
    }

    // top-k
    private List<DeveloperRecommendation> limit(List<DeveloperRecommendation> recommendations, int topK) {
        if (recommendations == null || recommendations.isEmpty()) return new ArrayList<>();
        int limit = Math.min(topK, recommendations.size());

        return new ArrayList<>(recommendations.subList(0, limit));
    }

    // no similarity
    private List<DeveloperRecommendation> buildZeroScoreRecommendations(List<User> developers, int topK) {
        List<DeveloperRecommendation> recommendations = new ArrayList<>();

        if (developers == null) {
            return recommendations;
        }

        for (User dev : developers) {
            if (dev != null && dev.isDev()) {
                recommendations.add(new DeveloperRecommendation(dev, 0.0, 0, 0));
            }
        }
        Collections.sort(recommendations);

        List<DeveloperRecommendation> result = limit(recommendations, topK);

        return result;
    }
}
