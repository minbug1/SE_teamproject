package its.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * DTO for issue statistics.
 *
 * UI는 이 객체를 받아서 출력만 담당한다.
 * 정렬, 표 형태 출력, 문자열 포맷팅은 UI 책임이다.
 */
public class IssueStatistics {

    private final int totalIssueCount;
    private final Map<IssueStatus, Integer> countByStatus;
    private final Map<LocalDate, Integer> dailyReportedIssueCount;
    private final Map<YearMonth, Integer> monthlyReportedIssueCount;
    private final Map<Priority, Integer> countByPriority;
    private final Map<Integer, Integer> countByCategory;
    private final Map<Integer, Integer> closedCountByCategory;
    private final double averageFixHours;
    private final int reopenedIssueCount;
    private final int totalReopenEventCount;
    private final Map<Integer, Integer> reopenCountByCategory;

    public IssueStatistics(
            int totalIssueCount,
            Map<IssueStatus, Integer> countByStatus,
            Map<LocalDate, Integer> dailyReportedIssueCount,
            Map<YearMonth, Integer> monthlyReportedIssueCount,
            Map<Priority, Integer> countByPriority,
            Map<Integer, Integer> countByCategory,
            Map<Integer, Integer> closedCountByCategory,
            double averageFixHours,
            int reopenedIssueCount,
            int totalReopenEventCount,
            Map<Integer, Integer> reopenCountByCategory
    ) {
        this.totalIssueCount = totalIssueCount;
        this.countByStatus = countByStatus;
        this.dailyReportedIssueCount = dailyReportedIssueCount;
        this.monthlyReportedIssueCount = monthlyReportedIssueCount;
        this.countByPriority = countByPriority;
        this.countByCategory = countByCategory;
        this.closedCountByCategory = closedCountByCategory;
        this.averageFixHours = averageFixHours;
        this.reopenedIssueCount = reopenedIssueCount;
        this.totalReopenEventCount = totalReopenEventCount;
        this.reopenCountByCategory = reopenCountByCategory;
    }

    public int getTotalIssueCount() {
        return totalIssueCount;
    }

    public Map<IssueStatus, Integer> getCountByStatus() {
        return countByStatus;
    }

    public Map<LocalDate, Integer> getDailyReportedIssueCount() {
        return dailyReportedIssueCount;
    }

    public Map<YearMonth, Integer> getMonthlyReportedIssueCount() {
        return monthlyReportedIssueCount;
    }

    public Map<Priority, Integer> getCountByPriority() {
        return countByPriority;
    }

    public Map<Integer, Integer> getCountByCategory() {
        return countByCategory;
    }

    public Map<Integer, Integer> getClosedCountByCategory() {
        return closedCountByCategory;
    }

    public double getAverageFixHours() {
        return averageFixHours;
    }

    public int getReopenedIssueCount() {
        return reopenedIssueCount;
    }

    public int getTotalReopenEventCount() {
        return totalReopenEventCount;
    }

    public Map<Integer, Integer> getReopenCountByCategory() {
        return reopenCountByCategory;
    }
}