package its.controller;

import its.model.DeveloperStatistics;
import its.model.Issue;
import its.model.IssueSimilarity;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Role;
import its.model.TFIDF;
import its.model.User;
import its.repository.IssueRepository;
import its.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Controller for developer recommendation.
 *
 * 추천 기준:
 * 1. CLOSED issue 기반 경험
 * 2. 현재 issue와 과거 CLOSED issue의 TF-IDF cosine similarity
 * 3. category 경험
 * 4. priority 경험
 * 5. 현재 assigned issue 수에 따른 workload penalty
 */
public class RecommendationController {

    private static final int DEFAULT_TOP_N = 3;

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final TFIDF tfidf;
    private final IssueSimilarity issueSimilarity;

    public RecommendationController(IssueRepository issueRepository,
                                    UserRepository userRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }

        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null.");
        }

        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.tfidf = new TFIDF();
        this.issueSimilarity = new IssueSimilarity();
    }

    /*
     * issueId를 기준으로 추천 developer 목록 반환.
     */
    public List<DeveloperRecommendation> recommendDevelopers(long issueId) {
        return recommendDevelopers(issueId, DEFAULT_TOP_N);
    }

    /*
     * issueId를 기준으로 상위 topN명의 developer 추천.
     */
    public List<DeveloperRecommendation> recommendDevelopers(long issueId, int topN) {
        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be positive.");
        }

        if (topN <= 0) {
            throw new IllegalArgumentException("Top N must be positive.");
        }

        Issue targetIssue = issueRepository.findById(issueId);

        if (targetIssue == null) {
            throw new IllegalArgumentException("Issue does not exist.");
        }

        return recommendDevelopers(targetIssue, topN);
    }

    /*
     * Issue 객체를 기준으로 developer 추천.
     *
     * 아직 repository에 저장되지 않은 새 issue를 대상으로도 추천할 수 있도록
     * Issue 객체를 직접 받는 메서드도 제공한다.
     */
    public List<DeveloperRecommendation> recommendDevelopers(Issue targetIssue, int topN) {
        if (targetIssue == null) {
            throw new IllegalArgumentException("Target issue must not be null.");
        }

        if (topN <= 0) {
            throw new IllegalArgumentException("Top N must be positive.");
        }

        List<Issue> issues = issueRepository.findAll();
        List<User> developers = userRepository.findByRole(Role.DEVELOPER);

        Map<Long, DeveloperStatistics> statisticsByDeveloper =
                buildDeveloperStatistics(developers, issues);

        /*
         * targetIssue가 repository에 이미 있는 issue라면 그대로 사용.
         * 새 issue라면 TF-IDF 계산을 위해 임시로 issues에 포함한다.
         */
        List<Issue> issuesForAnalysis = new ArrayList<>(issues);

        if (!containsIssue(issuesForAnalysis, targetIssue.getIssueId())) {
            issuesForAnalysis.add(targetIssue);
        }

        Map<Long, Map<String, Double>> tfIdfByIssue =
                tfidf.calculateTfIdfByIssue(issuesForAnalysis);

        Map<String, Double> targetVector = tfIdfByIssue.get(targetIssue.getIssueId());

        List<DeveloperRecommendation> recommendations = new ArrayList<>();

        for (User developer : developers) {
            DeveloperStatistics statistics =
                    statisticsByDeveloper.get(developer.getUserId());

            if (statistics == null) {
                continue;
            }

            double similarityScore =
                    calculateDeveloperSimilarityScore(developer, targetIssue, issues, tfIdfByIssue, targetVector);

            double categoryScore =
                    statistics.getCategoryExperienceRatio(targetIssue.getCategoryId());

            double priorityScore =
                    statistics.getPriorityExperienceRatio(targetIssue.getPriority());

            double experienceScore =
                    calculateExperienceScore(statistics);

            double workloadPenalty =
                    calculateWorkloadPenalty(statistics);

            double finalScore =
                    similarityScore * 0.45
                            + categoryScore * 0.25
                            + priorityScore * 0.15
                            + experienceScore * 0.15
                            - workloadPenalty;

            if (finalScore < 0.0) {
                finalScore = 0.0;
            }

            DeveloperRecommendation recommendation =
                    new DeveloperRecommendation(
                            developer,
                            finalScore,
                            similarityScore,
                            categoryScore,
                            priorityScore,
                            experienceScore,
                            workloadPenalty,
                            statistics
                    );

            recommendations.add(recommendation);
        }

        sortRecommendations(recommendations);

        return limitRecommendations(recommendations, topN);
    }

    /*
     * developer별 통계 생성.
     *
     * CLOSED issue는 경험 통계에 반영하고,
     * ASSIGNED issue는 현재 workload에 반영한다.
     */
    private Map<Long, DeveloperStatistics> buildDeveloperStatistics(List<User> developers,
                                                                    List<Issue> issues) {
        Map<Long, DeveloperStatistics> statisticsByDeveloper = new HashMap<>();

        if (developers == null) {
            return statisticsByDeveloper;
        }

        for (User developer : developers) {
            if (developer == null || !developer.isDev()) {
                continue;
            }

            statisticsByDeveloper.put(
                    developer.getUserId(),
                    new DeveloperStatistics(developer)
            );
        }

        if (issues == null) {
            return statisticsByDeveloper;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            /*
             * CLOSED issue 기반 경험 반영.
             */
            User fixer = issue.getFixer();

            if (fixer != null) {
                DeveloperStatistics statistics =
                        statisticsByDeveloper.get(fixer.getUserId());

                if (statistics != null) {
                    statistics.recordClosedIssue(issue);
                }
            }

            /*
             * 현재 assigned workload 반영.
             */
            User assignee = issue.getAssignee();

            if (assignee != null) {
                DeveloperStatistics statistics =
                        statisticsByDeveloper.get(assignee.getUserId());

                if (statistics != null) {
                    statistics.recordCurrentAssignedIssue(issue);
                }
            }
        }

        return statisticsByDeveloper;
    }

    /*
     * 특정 developer가 과거에 해결한 CLOSED issue들과
     * target issue 사이의 평균 cosine similarity 계산.
     *
     * 추천/통계 시스템은 CLOSED 기반으로 동작하므로
     * CLOSED issue만 사용한다.
     */
    private double calculateDeveloperSimilarityScore(User developer,
                                                     Issue targetIssue,
                                                     List<Issue> issues,
                                                     Map<Long, Map<String, Double>> tfIdfByIssue,
                                                     Map<String, Double> targetVector) {
        if (developer == null || targetIssue == null || issues == null) {
            return 0.0;
        }

        if (tfIdfByIssue == null || targetVector == null || targetVector.isEmpty()) {
            return 0.0;
        }

        double similaritySum = 0.0;
        int similarClosedIssueCount = 0;

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getStatus() != IssueStatus.CLOSED) {
                continue;
            }

            if (issue.getFixer() == null || !issue.getFixer().equals(developer)) {
                continue;
            }

            if (issue.getIssueId() == targetIssue.getIssueId()) {
                continue;
            }

            Map<String, Double> closedIssueVector =
                    tfIdfByIssue.get(issue.getIssueId());

            double similarity =
                    issueSimilarity.calculateCosineSimilarity(
                            targetVector,
                            closedIssueVector
                    );

            similaritySum += similarity;
            similarClosedIssueCount++;
        }

        if (similarClosedIssueCount == 0) {
            return 0.0;
        }

        return similaritySum / similarClosedIssueCount;
    }

    /*
     * closedCount 기반 단순 경험 점수.
     *
     * closedCount가 많을수록 증가하지만,
     * 너무 큰 값이 압도하지 않도록 완만하게 제한한다.
     */
    private double calculateExperienceScore(DeveloperStatistics statistics) {
        if (statistics == null) {
            return 0.0;
        }

        int closedCount = statistics.getClosedCount();

        if (closedCount <= 0) {
            return 0.0;
        }

        /*
         * closed issue 5개 이상이면 경험 점수는 1.0으로 본다.
         */
        return Math.min(1.0, closedCount / 5.0);
    }

    /*
     * 현재 assigned issue가 많을수록 penalty.
     */
    private double calculateWorkloadPenalty(DeveloperStatistics statistics) {
        if (statistics == null) {
            return 0.0;
        }

        return statistics.getCurrentAssignedCount() * 0.05;
    }

    private boolean containsIssue(List<Issue> issues, long issueId) {
        if (issues == null || issueId <= 0) {
            return false;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getIssueId() == issueId) {
                return true;
            }
        }

        return false;
    }

    private void sortRecommendations(List<DeveloperRecommendation> recommendations) {
        recommendations.sort((left, right) -> {
            int scoreCompare = Double.compare(right.getFinalScore(), left.getFinalScore());

            if (scoreCompare != 0) {
                return scoreCompare;
            }

            /*
             * 점수가 같으면 workload가 적은 developer 우선.
             */
            int workloadCompare = Integer.compare(
                    left.getStatistics().getCurrentAssignedCount(),
                    right.getStatistics().getCurrentAssignedCount()
            );

            if (workloadCompare != 0) {
                return workloadCompare;
            }

            /*
             * 그래도 같으면 closedCount가 많은 developer 우선.
             */
            return Integer.compare(
                    right.getStatistics().getClosedCount(),
                    left.getStatistics().getClosedCount()
            );
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

    /*
     * 추천 결과 DTO.
     *
     * controller 바깥에서 결과를 보기 좋게 사용하기 위한 내부 static class.
     * 필요하면 별도 model 클래스로 분리해도 된다.
     */
    public static class DeveloperRecommendation {

        private final User developer;
        private final double finalScore;
        private final double similarityScore;
        private final double categoryScore;
        private final double priorityScore;
        private final double experienceScore;
        private final double workloadPenalty;
        private final DeveloperStatistics statistics;

        public DeveloperRecommendation(User developer,
                                       double finalScore,
                                       double similarityScore,
                                       double categoryScore,
                                       double priorityScore,
                                       double experienceScore,
                                       double workloadPenalty,
                                       DeveloperStatistics statistics) {
            this.developer = developer;
            this.finalScore = finalScore;
            this.similarityScore = similarityScore;
            this.categoryScore = categoryScore;
            this.priorityScore = priorityScore;
            this.experienceScore = experienceScore;
            this.workloadPenalty = workloadPenalty;
            this.statistics = statistics;
        }

        public User getDeveloper() {
            return developer;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public double getCategoryScore() {
            return categoryScore;
        }

        public double getPriorityScore() {
            return priorityScore;
        }

        public double getExperienceScore() {
            return experienceScore;
        }

        public double getWorkloadPenalty() {
            return workloadPenalty;
        }

        public DeveloperStatistics getStatistics() {
            return statistics;
        }

        @Override
        public String toString() {
            return "DeveloperRecommendation{" +
                    "developer=" + developer.getLoginId() +
                    ", finalScore=" + finalScore +
                    ", similarityScore=" + similarityScore +
                    ", categoryScore=" + categoryScore +
                    ", priorityScore=" + priorityScore +
                    ", experienceScore=" + experienceScore +
                    ", workloadPenalty=" + workloadPenalty +
                    ", closedCount=" + statistics.getClosedCount() +
                    ", currentAssignedCount=" + statistics.getCurrentAssignedCount() +
                    '}';
        }
    }
}