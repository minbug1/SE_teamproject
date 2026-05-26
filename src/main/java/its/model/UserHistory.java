package its.model;

import java.util.HashMap;
import java.util.Map;

/**
 * user history for statistics and recommendation
 * 
 *
 * @author hanung
 */

public class UserHistory {

    private User user;
    // Tester
    private int reportedCount;
    private int resolvedCount;
    private int reopenedCount;
    private double averageResolveHours;
    // PL
    private int assignedCount;
    private int closedCount;
    // Developer
    private int fixedCount;
    private int currentAssignedCount;
    private double averageFixHours;
    // General
    private Map<Integer, Integer> countByCategory = new HashMap<>();
    private Map<Priority, Integer> countByPriority = new HashMap<>();

    // constructor
    public UserHistory(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        this.user = user;
    }

    // get
    public User getUser() {
        return user;
    }

    public int getReportedCount() {
        return reportedCount;
    }

    public int getResolvedCount() {
        return resolvedCount;
    }

    public int getReopenedCount() {
        return reopenedCount;
    }

    public double getAverageResolveHours() {
        return averageResolveHours;
    }

    public int getAssignedCount() {
        return assignedCount;
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

    public double getAverageFixHours() {
        return averageFixHours;
    }

    public Map<Integer, Integer> getCountByCategory() {
        return new HashMap<>(countByCategory);
    }

    public Map<Priority, Integer> getCountByPriority() {
        return new HashMap<>(countByPriority);
    }

    // increase
    public void increaseReportedCount() {
        reportedCount++;
    }

    public void increaseResolvedCount() {
        resolvedCount++;
    }

    public void increaseReopenedCount() {
        reopenedCount++;
    }

    public void increaseAssignedCount() {
        assignedCount++;
    }

    public void increaseClosedCount() {
        closedCount++;
    }

    public void increaseFixedCount() {
        fixedCount++;
    }

    public void increaseCurrentAssignedCount() {
        currentAssignedCount++;
    }

    public void increaseCategoryCount(int categoryId) {
        countByCategory.put(categoryId, countByCategory.getOrDefault(categoryId, 0) + 1);
    }

    public void increasePriorityCount(Priority priority) {
        if (priority == null) {
            return;
        }
        countByPriority.put(priority, countByPriority.getOrDefault(priority, 0) + 1);
    }

    // set
    public void setAverageResolveHours(double averageResolveHours) {
        this.averageResolveHours = averageResolveHours;
    }

    public void setAverageFixHours(double averageFixHours) {
        this.averageFixHours = averageFixHours;
    }
}