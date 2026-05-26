package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for developer recommendation.
 *
 * 추천 기준:
 * 1. target issue와 과거 CLOSED issue의 TF-IDF cosine similarity를 계산한다.
 * 2. similarity가 높은 CLOSED issue의 fixer에게 점수를 누적한다.
 * 3. 점수가 높은 developer를 추천한다.
 *
 * 주의:
 * - Jaccard similarity는 사용하지 않는다.
 * - Jaccard는 초기 category clustering에서만 사용한다.
 * - 이 클래스는 repository를 직접 알지 않는다.
 * - IssueController가 issue 목록과 developer 목록을 넘겨준다.
 *
 * @author hanung
 */
public class RecommendationEngine {

    private static final int DEFAULT_TOP_N = 3;

    private final TFIDF tfidf;
    private final IssueSimilarity issueSimilarity;

    public RecommendationEngine() {
        this.tfidf = new TFIDF();
        this.issueSimilarity = new IssueSimilarity();
    }

    /**
     * 기본값으로 상위 3명의 developer를 추천한다.
     */
    public List<DeveloperRecommendation> recommendDevelopers(
            Issue targetIssue,
            List<Issue> allIssues,
            List<User> developers
    ) {
        return recommendDevelopers(targetIssue, allIssues, developers, DEFAULT_TOP_N);
    }

    /**
     * target issue에 대해 상위 topN명의 developer를 추천한다.
     *
     * @param targetIssue 추천 대상 issue
     * @param allIssues 전체 issue 목록
     * @param developers developer user 목록
     * @param topN 추천할 developer 수
     * @return 추천 결과 목록
     */
    public List<DeveloperRecommendation> recommendDevelopers(
            Issue targetIssue,
            List<Issue> allIssues,
            List<User> developers,
            int topN
    ) {
        validateInput(targetIssue, allIssues, developers, topN);

        List<User> validDevelopers = filterDevelopers(developers);

        if (validDevelopers.isEmpty()) {
            return new ArrayList<>();
        }

        /*
         * TF-IDF 계산을 위해 targetIssue가 allIssues에 없으면 임시로 포함한다.
         * 예: 아직 저장 전인 issue를 대상으로 추천할 때도 동작 가능.
         */
        List<Issue> issuesForAnalysis = new ArrayList<>(allIssues);

        if (!containsIssue(issuesForAnalysis, targetIssue.getIssueId())) {
            issuesForAnalysis.add(targetIssue);
        }

        /*
         * issueId -> TF-IDF vector
         */
        Map<Long, Map<String, Double>> vectorByIssue =
                tfidf.calculateTfIdfByIssue(issuesForAnalysis);

        Map<String, Double> targetVector =
                vectorByIssue.get(targetIssue.getIssueId());

        /*
         * 현재 TFIDF.java는 IDF를 category 기반으로 계산한다.
         * 따라서 category가 아직 충분히 지정되지 않은 초기 단계에서는
         * TF-IDF vector가 비어 있을 수 있다.
         *
         * 이 경우에는 추천이 완전히 죽지 않도록 TF vector로 fallback한다.
         */
        if (targetVector == null || targetVector.isEmpty()) {
            vectorByIssue = tfidf.calculateTfByIssue(issuesForAnalysis);
            targetVector = vectorByIssue.get(targetIssue.getIssueId());
        }

        Map<Long, User> developerById = buildDeveloperMap(validDevelopers);
        Map<Long, Double> scoreByDeveloperId = initializeScoreMap(validDevelopers);
        Map<Long, Integer> matchedClosedCountByDeveloperId = initializeCountMap(validDevelopers);
        Map<Long, Integer> totalClosedCountByDeveloperId = initializeCountMap(validDevelopers);

        /*
         * CLOSED issue만 추천 이력으로 사용한다.
         * CLOSED issue의 fixer가 실제 해결 경험을 가진 developer라고 본다.
         */
        for (Issue pastIssue : allIssues) {
            if (!isUsableClosedIssue(pastIssue, targetIssue)) {
                continue;
            }

            User fixer = pastIssue.getFixer();

            if (fixer == null) {
                continue;
            }

            long fixerId = fixer.getUserId();

            /*
             * developer 목록에 없는 fixer는 추천 후보에서 제외한다.
             */
            if (!developerById.containsKey(fixerId)) {
                continue;
            }

            totalClosedCountByDeveloperId.put(
                    fixerId,
                    totalClosedCountByDeveloperId.getOrDefault(fixerId, 0) + 1
            );

            Map<String, Double> pastVector =
                    vectorByIssue.get(pastIssue.getIssueId());

            double similarity =
                    issueSimilarity.calculateCosineSimilarity(targetVector, pastVector);

            /*
             * similarity가 0이면 target issue와 텍스트상 관련이 거의 없다고 본다.
             * totalClosedCount에는 이미 반영했지만 추천 점수에는 반영하지 않는다.
             */
            if (similarity <= 0.0) {
                continue;
            }

            scoreByDeveloperId.put(
                    fixerId,
                    scoreByDeveloperId.getOrDefault(fixerId, 0.0) + similarity
            );

            matchedClosedCountByDeveloperId.put(
                    fixerId,
                    matchedClosedCountByDeveloperId.getOrDefault(fixerId, 0) + 1
            );
        }

        List<DeveloperRecommendation> recommendations = new ArrayList<>();

        for (User developer : validDevelopers) {
            long developerId = developer.getUserId();

            double score = scoreByDeveloperId.getOrDefault(developerId, 0.0);
            int matchedClosedCount =
                    matchedClosedCountByDeveloperId.getOrDefault(developerId, 0);
            int totalClosedCount =
                    totalClosedCountByDeveloperId.getOrDefault(developerId, 0);

            DeveloperRecommendation recommendation =
                    new DeveloperRecommendation(
                            developer,
                            score,
                            matchedClosedCount,
                            totalClosedCount
                    );

            recommendations.add(recommendation);
        }

        sortRecommendations(recommendations);

        return limitRecommendations(recommendations, topN);
    }

    private void validateInput(
            Issue targetIssue,
            List<Issue> allIssues,
            List<User> developers,
            int topN
    ) {
        if (targetIssue == null) {
            throw new IllegalArgumentException("Target issue must not be null.");
        }

        if (allIssues == null) {
            throw new IllegalArgumentException("Issue list must not be null.");
        }

        if (developers == null) {
            throw new IllegalArgumentException("Developer list must not be null.");
        }

        if (topN <= 0) {
            throw new IllegalArgumentException("Top N must be positive.");
        }
    }

    private List<User> filterDevelopers(List<User> users) {
        List<User> developers = new ArrayList<>();

        if (users == null) {
            return developers;
        }

        for (User user : users) {
            if (user == null) {
                continue;
            }

            if (user.getRole() != Role.DEVELOPER) {
                continue;
            }

            developers.add(user);
        }

        return developers;
    }

    private Map<Long, User> buildDeveloperMap(List<User> developers) {
        Map<Long, User> developerById = new HashMap<>();

        if (developers == null) {
            return developerById;
        }

        for (User developer : developers) {
            if (developer == null) {
                continue;
            }

            developerById.put(developer.getUserId(), developer);
        }

        return developerById;
    }

    private Map<Long, Double> initializeScoreMap(List<User> developers) {
        Map<Long, Double> scoreByDeveloperId = new HashMap<>();

        if (developers == null) {
            return scoreByDeveloperId;
        }

        for (User developer : developers) {
            if (developer == null) {
                continue;
            }

            scoreByDeveloperId.put(developer.getUserId(), 0.0);
        }

        return scoreByDeveloperId;
    }

    private Map<Long, Integer> initializeCountMap(List<User> developers) {
        Map<Long, Integer> countByDeveloperId = new HashMap<>();

        if (developers == null) {
            return countByDeveloperId;
        }

        for (User developer : developers) {
            if (developer == null) {
                continue;
            }

            countByDeveloperId.put(developer.getUserId(), 0);
        }

        return countByDeveloperId;
    }

    private boolean containsIssue(List<Issue> issues, long issueId) {
        if (issues == null || issueId <= 0) {
            return false;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getIssueId() == issueId) {
                return true;
            }
        }

        return false;
    }

    private boolean isUsableClosedIssue(Issue pastIssue, Issue targetIssue) {
        if (pastIssue == null || targetIssue == null) {
            return false;
        }

        /*
         * 추천 기준은 CLOSED issue만 사용한다.
         */
        if (pastIssue.getStatus() != IssueStatus.CLOSED) {
            return false;
        }

        /*
         * 자기 자신은 비교 대상에서 제외한다.
         */
        if (pastIssue.getIssueId() == targetIssue.getIssueId()) {
            return false;
        }

        /*
         * fixer가 없으면 어떤 developer에게 점수를 줄 수 없다.
         */
        if (pastIssue.getFixer() == null) {
            return false;
        }

        return true;
    }

    private void sortRecommendations(List<DeveloperRecommendation> recommendations) {
        recommendations.sort((left, right) -> {
            /*
             * 1순위: 추천 점수가 높은 developer
             */
            int scoreCompare =
                    Double.compare(right.getScore(), left.getScore());

            if (scoreCompare != 0) {
                return scoreCompare;
            }

            /*
             * 2순위: target issue와 유사한 CLOSED issue를 더 많이 해결한 developer
             */
            int matchedCompare =
                    Integer.compare(
                            right.getMatchedClosedIssueCount(),
                            left.getMatchedClosedIssueCount()
                    );

            if (matchedCompare != 0) {
                return matchedCompare;
            }

            /*
             * 3순위: 전체 CLOSED issue 해결 경험이 많은 developer
             */
            int totalClosedCompare =
                    Integer.compare(
                            right.getTotalClosedIssueCount(),
                            left.getTotalClosedIssueCount()
                    );

            if (totalClosedCompare != 0) {
                return totalClosedCompare;
            }

            /*
             * 4순위: loginId 오름차순
             * 점수가 모두 0인 초기 상황에서도 출력 순서를 안정적으로 만들기 위함.
             */
            String leftLoginId = left.getDeveloper().getLoginId();
            String rightLoginId = right.getDeveloper().getLoginId();

            if (leftLoginId == null && rightLoginId == null) {
                return 0;
            }

            if (leftLoginId == null) {
                return 1;
            }

            if (rightLoginId == null) {
                return -1;
            }

            return leftLoginId.compareTo(rightLoginId);
        });
    }

    private List<DeveloperRecommendation> limitRecommendations(
            List<DeveloperRecommendation> recommendations,
            int topN
    ) {
        List<DeveloperRecommendation> result = new ArrayList<>();

        if (recommendations == null || recommendations.isEmpty()) {
            return result;
        }

        int limit = Math.min(topN, recommendations.size());

        for (int i = 0; i < limit; i++) {
            result.add(recommendations.get(i));
        }

        return result;
    }

    /**
     * 추천 결과 DTO.
     *
     * 필요하면 나중에 별도 파일로 분리해도 되지만,
     * 현재는 RecommendationEngine 내부 static class로 두는 것이 간단하다.
     */
    public static class DeveloperRecommendation {

        private final User developer;
        private final double score;
        private final int matchedClosedIssueCount;
        private final int totalClosedIssueCount;

        public DeveloperRecommendation(
                User developer,
                double score,
                int matchedClosedIssueCount,
                int totalClosedIssueCount
        ) {
            if (developer == null) {
                throw new IllegalArgumentException("Developer must not be null.");
            }

            this.developer = developer;
            this.score = score;
            this.matchedClosedIssueCount = matchedClosedIssueCount;
            this.totalClosedIssueCount = totalClosedIssueCount;
        }

        public User getDeveloper() {
            return developer;
        }

        public double getScore() {
            return score;
        }

        public int getMatchedClosedIssueCount() {
            return matchedClosedIssueCount;
        }

        public int getTotalClosedIssueCount() {
            return totalClosedIssueCount;
        }

        @Override
        public String toString() {
            return "DeveloperRecommendation{" +
                    "developer=" + developer.getLoginId() +
                    ", score=" + score +
                    ", matchedClosedIssueCount=" + matchedClosedIssueCount +
                    ", totalClosedIssueCount=" + totalClosedIssueCount +
                    '}';
        }
    }
}