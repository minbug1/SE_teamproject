package its.repository;

import java.util.ArrayList;
import java.util.List;

import its.model.Issue;

public class FileIssueRepository implements IssueRepository {

    private List<Issue> issues = new ArrayList<>();

    @Override
    public void save(Issue issue) {
        validateIssue(issue);

        for (Issue existingIssue : issues) {
            if (existingIssue.getIssueId().equals(issue.getIssueId())) {
                throw new IllegalArgumentException("Issue ID already exists.");
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
            if (issue.getIssueId() == issueId) {
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
    public void update(Issue issue) {
        validateIssue(issue);

        boolean updated = false;

        for (int i = 0; i < issues.size(); i++) {
            Issue existingIssue = issues.get(i);

            if (existingIssue.getIssueId().equals(issue.getIssueId())) {
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
            if (issues.get(i).getIssueId() == issueId) {
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
            if (issue.getIssueId() > maxId) {
                maxId = issue.getIssueId();
            }
        }

        return maxId + 1;
    }

    private void validateIssue(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue must not be null.");
        }

        if (issue.getIssueId() == null || issue.getIssueId() <= 0) {
            throw new IllegalArgumentException("Issue ID must be positive.");
        }
    }
}