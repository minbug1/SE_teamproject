package its.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for calculating issue statistics.
 *
 * 실제 통계 계산은 controller가 아니라 model에서 수행한다.
 */
public class StatisticsEngine {

    public IssueStatistics calculate(List<Issue> issues) {
        if (issues == null) {
            throw new IllegalArgumentException("Issue list must not be null.");
        }

        return new IssueStatistics(
                countAllIssues(issues),
                calculateIssueCountByStatus(issues),
                calculateDailyReportedIssueCount(issues),
                calculateMonthlyReportedIssueCount(issues),
                calculateIssueCountByPriority(issues),
                calculateIssueCountByCategory(issues),
                calculateClosedIssueCountByCategory(issues),
                calculateAverageFixHours(issues),
                countReopenedIssues(issues),
                countTotalReopenEvents(issues),
                calculateReopenCountByCategory(issues)
        );
    }

    public int countAllIssues(List<Issue> issues) {
        if (issues == null) {
            return 0;
        }

        return issues.size();
    }

    public Map<IssueStatus, Integer> calculateIssueCountByStatus(List<Issue> issues) {
        Map<IssueStatus, Integer> result = initializeStatusMap();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null || issue.getStatus() == null) {
                continue;
            }

            IssueStatus status = issue.getStatus();
            result.put(status, result.get(status) + 1);
        }

        return result;
    }

    public Map<LocalDate, Integer> calculateDailyReportedIssueCount(List<Issue> issues) {
        Map<LocalDate, Integer> result = new HashMap<>();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null || issue.getReportedDate() == null) {
                continue;
            }

            LocalDate date = issue.getReportedDate().toLocalDate();

            result.put(
                    date,
                    result.getOrDefault(date, 0) + 1
            );
        }

        return result;
    }

    public Map<YearMonth, Integer> calculateMonthlyReportedIssueCount(List<Issue> issues) {
        Map<YearMonth, Integer> result = new HashMap<>();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null || issue.getReportedDate() == null) {
                continue;
            }

            YearMonth month = YearMonth.from(issue.getReportedDate());

            result.put(
                    month,
                    result.getOrDefault(month, 0) + 1
            );
        }

        return result;
    }

    public Map<Priority, Integer> calculateIssueCountByPriority(List<Issue> issues) {
        Map<Priority, Integer> result = initializePriorityMap();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null || issue.getPriority() == null) {
                continue;
            }

            Priority priority = issue.getPriority();
            result.put(priority, result.get(priority) + 1);
        }

        return result;
    }

    public Map<Integer, Integer> calculateIssueCountByCategory(List<Issue> issues) {
        Map<Integer, Integer> result = new HashMap<>();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            int categoryId = issue.getCategoryId();

            result.put(
                    categoryId,
                    result.getOrDefault(categoryId, 0) + 1
            );
        }

        return result;
    }

    public Map<Integer, Integer> calculateClosedIssueCountByCategory(List<Issue> issues) {
        Map<Integer, Integer> result = new HashMap<>();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getStatus() != IssueStatus.CLOSED) {
                continue;
            }

            int categoryId = issue.getCategoryId();

            result.put(
                    categoryId,
                    result.getOrDefault(categoryId, 0) + 1
            );
        }

        return result;
    }

    public double calculateAverageFixHours(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0.0;
        int count = 0;

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getStatus() != IssueStatus.CLOSED) {
                continue;
            }

            if (issue.getAssignedDate() == null ||
                    issue.getFixedDate() == null) {
                continue;
            }

            if (issue.getFixedDate().isBefore(issue.getAssignedDate())) {
                continue;
            }

            totalHours += calculateHoursBetween(
                    issue.getAssignedDate(),
                    issue.getFixedDate()
            );

            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        return totalHours / count;
    }

    public int countReopenedIssues(List<Issue> issues) {
        if (issues == null) {
            return 0;
        }

        int count = 0;

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getReopenCount() > 0 ||
                    issue.getStatus() == IssueStatus.REOPENED) {
                count++;
            }
        }

        return count;
    }

    public int countTotalReopenEvents(List<Issue> issues) {
        if (issues == null) {
            return 0;
        }

        int total = 0;

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            total += issue.getReopenCount();
        }

        return total;
    }

    public Map<Integer, Integer> calculateReopenCountByCategory(List<Issue> issues) {
        Map<Integer, Integer> result = new HashMap<>();

        if (issues == null) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getReopenCount() <= 0) {
                continue;
            }

            int categoryId = issue.getCategoryId();

            result.put(
                    categoryId,
                    result.getOrDefault(categoryId, 0) + issue.getReopenCount()
            );
        }

        return result;
    }

    private Map<IssueStatus, Integer> initializeStatusMap() {
        Map<IssueStatus, Integer> map =
                new EnumMap<>(IssueStatus.class);

        for (IssueStatus status : IssueStatus.values()) {
            map.put(status, 0);
        }

        return map;
    }

    private Map<Priority, Integer> initializePriorityMap() {
        Map<Priority, Integer> map =
                new EnumMap<>(Priority.class);

        for (Priority priority : Priority.values()) {
            map.put(priority, 0);
        }

        return map;
    }

    private double calculateHoursBetween(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    ) {
        Duration duration = Duration.between(start, end);
        return duration.toMinutes() / 60.0;
    }
}