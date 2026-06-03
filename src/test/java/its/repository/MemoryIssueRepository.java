package its.repository;

import its.model.Issue;

import java.util.ArrayList;
import java.util.List;

/*
 * Test-only in-memory implementation
 * Override FileIssueRepository
 *
 *
 */
public class MemoryIssueRepository implements IssueRepository {

    // memory storage
    private final List<Issue> issues = new ArrayList<>();

    @Override
    public void save(Issue issue) {
        validateIssue(issue);

        for (Issue existingIssue : issues) {
            if (isSameIssue(existingIssue, issue)) {
                throw new IllegalArgumentException("Issue ID already exists in this project.");
            }
        }

        issues.add(issue);
    }

    @Override
    public Issue findById(long issueId) {
        if (issueId <= 0) {
            return null;
        }

        for (Issue issue : issues) {
            if (issue.getIssueId() != 0 && issue.getIssueId() == issueId) {
                return issue;
            }
        }

        return null;
    }

    @Override
    public Issue findByProjectIdAndIssueId(int projectId, long issueId) {
        if (projectId <= 0 || issueId <= 0) {
            return null;
        }

        for (Issue issue : issues) {
            if ((issue.getProjectId() == projectId || issue.getProjectId() == 0)
                    && issue.getIssueId() != 0
                    && issue.getIssueId() == issueId) {
                return issue;
            }
        }

        return null;
    }

    @Override
    public List<Issue> findAll() {
        return new ArrayList<>(issues);
    }

    @Override
    public List<Issue> findByProjectId(int projectId) {
        List<Issue> result = new ArrayList<>();

        if (projectId <= 0) {
            return result;
        }

        for (Issue issue : issues) {
            if (issue.getProjectId() == projectId) {
                result.add(issue);
            }
        }

        return result;
    }

    @Override
    public void update(Issue issue) {
        validateIssue(issue);

        boolean updated = false;

        for (int i = 0; i < issues.size(); i++) {
            if (isSameIssue(issues.get(i), issue)) {
                issues.set(i, issue);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("Issue does not exist.");
        }
    }

    @Override
    public void delete(long issueId) {
        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be positive.");
        }

        boolean deleted = false;

        for (int i = 0; i < issues.size(); i++) {
            Issue issue = issues.get(i);

            if (issue.getIssueId() == issueId) {
                issues.remove(i);
                deleted = true;
                break;
            }
        }

        if (!deleted) {
            throw new IllegalArgumentException("Issue does not exist.");
        }
    }

    @Override
    public void delete(int projectId, long issueId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }

        if (issueId <= 0) {
            throw new IllegalArgumentException("Issue ID must be positive.");
        }

        boolean deleted = false;

        for (int i = 0; i < issues.size(); i++) {
            Issue issue = issues.get(i);

            if ((issue.getProjectId() == projectId || issue.getProjectId() == 0)
                    && issue.getIssueId() != 0
                    && issue.getIssueId() == issueId) {
                issues.remove(i);
                deleted = true;
                break;
            }
        }

        if (!deleted) {
            throw new IllegalArgumentException("Issue does not exist.");
        }
    }

    @Override
    public long generateIssueId() {
        long maxId = 0;

        for (Issue issue : issues) {
            if (issue.getIssueId() != 0 && issue.getIssueId() > maxId) {
                maxId = issue.getIssueId();
            }
        }

        return maxId + 1;
    }

    @Override
    public long generateIssueId(int projectId) {
        long maxId = 0;

        for (Issue issue : issues) {
            if (issue.getProjectId() == projectId
                    && issue.getIssueId() != 0
                    && issue.getIssueId() > maxId) {
                maxId = issue.getIssueId();
            }
        }

        return maxId + 1;
    }

    private boolean isSameIssue(Issue left, Issue right) {
        return left.getIssueId() != 0
                && right.getIssueId() != 0
                && left.getIssueId() == right.getIssueId()
                && (left.getProjectId() == right.getProjectId()
                        || left.getProjectId() == 0
                        || right.getProjectId() == 0);
    }

    private void validateIssue(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue must not be null.");
        }

        if (issue.getIssueId() <= 0) {
            throw new IllegalArgumentException("Issue ID must be positive.");
        }
    }
}