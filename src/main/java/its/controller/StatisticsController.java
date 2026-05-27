package its.controller;

import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.Status;
import its.model.User;
import its.repository.IssueRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsController {
    private final IssueRepository issueRepository;

    public StatisticsController(IssueRepository issueRepository) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }
        this.issueRepository = issueRepository;
    }

    public Map<Integer, Long> getIssueCountByDay(int projectId, int year, int month) {
        validateProjectId(projectId);

        Map<Integer, Long> result = new LinkedHashMap<>();
        int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            result.put(d, 0L);
        }

        for (Issue issue : getProjectIssues(projectId)) {
            LocalDateTime date = issue.getReportedDate();
            if (date != null && date.getYear() == year && date.getMonthValue() == month) {
                int day = date.getDayOfMonth();
                result.put(day, result.get(day) + 1);
            }
        }
        return result;
    }

    public Map<Integer, Long> getIssueCountByMonth(int projectId, int year) {
        validateProjectId(projectId);
 
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            result.put(m, 0L);
        }
 
        for (Issue issue : getProjectIssues(projectId)) {
            LocalDateTime date = issue.getReportedDate();
            if (date != null && date.getYear() == year) {
                int month = date.getMonthValue();
                result.put(month, result.get(month) + 1);
            }
        }
        return result;
    }

    public Map<Priority, Long> getIssueCountByPriority(int projectId) {
        validateProjectId(projectId);
 
        Map<Priority, Long> result = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            result.put(p, 0L);
        }
 
        for (Issue issue : getProjectIssues(projectId)) {
            if (issue.getPriority() != null) {
                result.put(issue.getPriority(), result.get(issue.getPriority()) + 1);
            }
        }
        return result;
    }

    public Map<User, Long> getResolvedCountByDeveloper(int projectId) {
        validateProjectId(projectId);
 
        Map<User, Long> result = new LinkedHashMap<>();
        Set<Status> resolvedStatuses = EnumSet.of(Status.FIXED, Status.RESOLVED, Status.CLOSED);
 
        for (Issue issue : getProjectIssues(projectId)) {
            if (issue.getFixer() != null && resolvedStatuses.contains(issue.getStatus())) {
                result.merge(issue.getFixer(), 1L, Long::sum);
            }
        }
        return result;
    }

    public List<Integer> getAvailableYears(int projectId) {
        validateProjectId(projectId);
 
        Set<Integer> years = new TreeSet<>();
        for (Issue issue : getProjectIssues(projectId)) {
            if (issue.getReportedDate() != null) {
                years.add(issue.getReportedDate().getYear());
            }
        }
        // 이슈가 없으면 올해를 기본값으로
        if (years.isEmpty()) {
            years.add(LocalDateTime.now().getYear());
        }
        return new ArrayList<>(years);
    }

    private List<Issue> getProjectIssues(int projectId) {
        return issueRepository.findByProjectId(projectId);
    }
 
    private void validateProjectId(int projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be a positive number.");
        }
    }
}
