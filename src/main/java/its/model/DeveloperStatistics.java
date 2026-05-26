package its.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DeveloperStatistics {

    private User developer;

    /*
     * CLOSED issue 기반 통계
     */
    private int closedCount;
    private int fixedCount;

    /*
     * 현재 담당 중인 issue 수
     * 추천 시 workload penalty에 사용할 수 있다.
     */
    private int currentAssignedCount;

    /*
     * 시간 통계
     */
    private double totalFixHours;
    private double totalResolveHours;
    private double averageFixHours;
    private double averageResolveHours;

    /*
     * category별 경험
     * key   : categoryId
     * value : 해당 category에서 closed 처리한 issue 수
     */
    private Map<Integer, Integer> closedCountByCategory = new HashMap<>();

    /*
     * priority별 경험
     */
    private Map<Priority, Integer> closedCountByPriority = new HashMap<>();

    public DeveloperStatistics(User developer) {
        if (developer == null) {
            throw new IllegalArgumentException("Developer must not be null.");
        }

        if (!developer.isDev()) {
            throw new IllegalArgumentException("User must have DEVELOPER role.");
        }

        this.developer = developer;
    }

    public User getDeveloper() {
        return developer;
    }

    public long getDeveloperId() {
        return developer.getUserId();
    }

    public String getDeveloperLoginId() {
        return developer.getLoginId();
    }

    public int getClosedCount() {
        return closedCount;
    }

    public int getFixedCount() {
        return fixedCount;
    }

    public int getCurrentAssignedCount() {
        return currentAssignedCount;
    }

    public double getTotalFixHours() {
        return totalFixHours;
    }

    public double getTotalResolveHours() {
        return totalResolveHours;
    }

    public double getAverageFixHours() {
        return averageFixHours;
    }

    public double getAverageResolveHours() {
        return averageResolveHours;
    }

    public Map<Integer, Integer> getClosedCountByCategory() {
        return new HashMap<>(closedCountByCategory);
    }

    public Map<Priority, Integer> getClosedCountByPriority() {
        return new HashMap<>(closedCountByPriority);
    }

    /*
     * CLOSED issue 하나를 developer 통계에 반영한다.
     *
     * 추천/통계 시스템이 CLOSED 기반이라면
     * 가장 중요한 누적 메서드가 된다.
     */
    public void recordClosedIssue(Issue issue) {
        if (issue == null) {
            return;
        }

        if (issue.getStatus() != IssueStatus.CLOSED) {
            return;
        }

        /*
         * fixer가 이 developer인 경우만 통계에 반영한다.
         */
        if (issue.getFixer() == null || !issue.getFixer().equals(developer)) {
            return;
        }

        closedCount++;
        fixedCount++;

        increaseCategoryCount(issue.getCategoryId());
        increasePriorityCount(issue.getPriority());

        addFixHours(issue);
        addResolveHours(issue);

        updateAverages();
    }

    /*
     * 현재 ASSIGNED 상태인 issue를 workload로 반영한다.
     *
     * 이 값은 추천 시 현재 일이 많은 developer에게
     * 낮은 점수를 주는 데 사용할 수 있다.
     */
    public void recordCurrentAssignedIssue(Issue issue) {
        if (issue == null) {
            return;
        }

        if (issue.getStatus() != IssueStatus.ASSIGNED) {
            return;
        }

        if (issue.getAssignee() == null || !issue.getAssignee().equals(developer)) {
            return;
        }

        currentAssignedCount++;
    }

    /*
     * category 경험 수 반환.
     */
    public int getClosedCountForCategory(int categoryId) {
        return closedCountByCategory.getOrDefault(categoryId, 0);
    }

    /*
     * priority 경험 수 반환.
     */
    public int getClosedCountForPriority(Priority priority) {
        if (priority == null) {
            return 0;
        }

        return closedCountByPriority.getOrDefault(priority, 0);
    }

    /*
     * 해당 category에 대한 경험 비율.
     *
     * 예:
     * closedCount = 10
     * category 1 closed count = 4
     * => 0.4
     */
    public double getCategoryExperienceRatio(int categoryId) {
        if (closedCount == 0) {
            return 0.0;
        }

        return (double) getClosedCountForCategory(categoryId) / closedCount;
    }

    /*
     * 해당 priority에 대한 경험 비율.
     */
    public double getPriorityExperienceRatio(Priority priority) {
        if (closedCount == 0) {
            return 0.0;
        }

        return (double) getClosedCountForPriority(priority) / closedCount;
    }

    /*
     * 추천 점수 계산용 기본 경험 점수.
     *
     * 이 메서드는 매우 단순한 기본형이다.
     * 나중에 RecommendationController에서
     * similarity, workload, averageFixHours 등을 조합해도 된다.
     */
    public double calculateBasicRecommendationScore(int categoryId, Priority priority) {
        double categoryScore = getCategoryExperienceRatio(categoryId);
        double priorityScore = getPriorityExperienceRatio(priority);

        /*
         * 현재 맡은 issue가 많을수록 약한 penalty.
         */
        double workloadPenalty = currentAssignedCount * 0.05;

        double score = categoryScore * 0.7
                + priorityScore * 0.3
                - workloadPenalty;

        if (score < 0.0) {
            return 0.0;
        }

        return score;
    }

    private void increaseCategoryCount(int categoryId) {
        if (categoryId <= 0) {
            return;
        }

        closedCountByCategory.put(
                categoryId,
                closedCountByCategory.getOrDefault(categoryId, 0) + 1
        );
    }

    private void increasePriorityCount(Priority priority) {
        if (priority == null) {
            return;
        }

        closedCountByPriority.put(
                priority,
                closedCountByPriority.getOrDefault(priority, 0) + 1
        );
    }

    /*
     * assignedDate -> fixedDate 기준 fix 시간 계산.
     */
    private void addFixHours(Issue issue) {
        LocalDateTime assignedDate = issue.getAssignedDate();
        LocalDateTime fixedDate = issue.getFixedDate();

        if (assignedDate == null || fixedDate == null) {
            return;
        }

        if (fixedDate.isBefore(assignedDate)) {
            return;
        }

        totalFixHours += calculateHoursBetween(assignedDate, fixedDate);
    }

    /*
     * reportedDate -> resolvedDate 기준 resolve 시간 계산.
     *
     * resolvedDate가 없으면 closedDate를 대신 사용한다.
     */
    private void addResolveHours(Issue issue) {
        LocalDateTime reportedDate = issue.getReportedDate();
        LocalDateTime resolvedDate = issue.getResolvedDate();

        if (resolvedDate == null) {
            resolvedDate = issue.getClosedDate();
        }

        if (reportedDate == null || resolvedDate == null) {
            return;
        }

        if (resolvedDate.isBefore(reportedDate)) {
            return;
        }

        totalResolveHours += calculateHoursBetween(reportedDate, resolvedDate);
    }

    private double calculateHoursBetween(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return duration.toMinutes() / 60.0;
    }

    private void updateAverages() {
        if (closedCount == 0) {
            averageFixHours = 0.0;
            averageResolveHours = 0.0;
            return;
        }

        averageFixHours = totalFixHours / closedCount;
        averageResolveHours = totalResolveHours / closedCount;
    }

    @Override
    public String toString() {
        return "DeveloperStatistics{" +
                "developer=" + developer.getLoginId() +
                ", closedCount=" + closedCount +
                ", fixedCount=" + fixedCount +
                ", currentAssignedCount=" + currentAssignedCount +
                ", averageFixHours=" + averageFixHours +
                ", averageResolveHours=" + averageResolveHours +
                ", closedCountByCategory=" + closedCountByCategory +
                ", closedCountByPriority=" + closedCountByPriority +
                '}';
    }
}