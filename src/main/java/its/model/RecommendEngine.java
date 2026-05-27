package its.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public RecommendEngine() {

    }

    // default top-k
    public List<DeveloperRecommendation> recommendDevelopers(
            Issue targetIssue, List<Issue> Issues, List<User> developers) {
        return recommendDevelopers(targetIssue, Issues, developers, DEFAULT_TOP_N);
    }

    // manual top-k
    public List<DeveloperRecommendation> recommendDevelopers(
            Issue targetIssue, List<Issue> Issues, List<User> developers, int topK) {
        
        if (targetIssue == null || Issues == null || developers == null || topK <= 0) {
            return new ArrayList<>();
        }

        // uncategoriezed -> recommend block
        if (isUncategorized(Issues)) {
            return new ArrayList<>(); 
        }

        // add target issue
        List<Issue> issueList = new ArrayList<>(Issues);
        if (!containsIssue(issueList, targetIssue.getIssueId())) {
            issueList.add(targetIssue);
        }

        Map<Long, Map<String, Double>> tfIdfVector = tfIdf.calculateTfIdfByIssue(issueList);
        Map<String, Double> targetVector = tfIdfVector.get(targetIssue.getIssueId());

        // no similarity
        if (targetVector == null || targetVector.isEmpty()) {
            List<DeveloperRecommendation> result = buildZeroScoreRecommendations(developers, topK);

            return result;
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

        for (Issue pastIssue : Issues) {
            if (pastIssue == null) {
                continue;
            }

            // count assigned workload
            if (pastIssue.getStatus() == IssueStatus.ASSIGNED && pastIssue.getAssignee() != null) {
                long currentAssigneeId = pastIssue.getAssignee().getUserId();
                if (assignedCountByDeveloperId.containsKey(currentAssigneeId)) {
                    assignedCountByDeveloperId.put(currentAssigneeId, assignedCountByDeveloperId.get(currentAssigneeId) + 1);
                }
            }

            // valid check
            if (!isValidIssue(pastIssue, targetIssue)) {
                continue;
            }

            // find fixer
            User fixer = pastIssue.getFixer();
            if (fixer == null || !developerById.containsKey(fixer.getUserId())){
                continue;
            }

            // resolvedIssueCount
            long fixerId = fixer.getUserId();
            resolvedIssueCountByDeveloperId.put(fixerId, resolvedIssueCountByDeveloperId.get(fixerId) + 1);

            // similarity
            Map<String, Double> pastVector = tfIdfVector.get(pastIssue.getIssueId());
            double similarity = tfIdf.cosineSimilarity(targetVector, pastVector);

            if (similarity <= 0.0) {
                continue;
            }

            // similarity score
            scoreByDeveloperId.put(fixerId, scoreByDeveloperId.get(fixerId) + similarity);
            matchedIssueCountByDeveloperId.put(fixerId, matchedIssueCountByDeveloperId.get(fixerId) + 1);
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

    // is uncategorized
    private boolean isUncategorized(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return true;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getCategoryId() > 0) {
                return false;
            }
        }

        return true;
    }

    // valid check
    private boolean isValidIssue(Issue pastIssue, Issue targetIssue) {
        if (pastIssue == null || targetIssue == null) {
            return false;
        }

        // issue itself
        if (pastIssue.getIssueId() == targetIssue.getIssueId()) {
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

    // contain tnf
    private boolean containsIssue(List<Issue> issues, long issueId) {
        if (issues == null) {
            return false;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getIssueId() == issueId) {
                return true;
            }
        }

        return false;
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