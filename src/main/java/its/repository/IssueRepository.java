package its.repository;

import java.util.List;

import its.model.Issue;

public interface IssueRepository {

    void save(Issue issue);
    Issue findById(long issueId);
    Issue findByProjectIdAndIssueId(int projectId, long issueId);
    List<Issue> findAll();
    List<Issue> findByProjectId(int projectId);

    void update(Issue issue);
    void delete(long issueId);
    void delete(int projectId, long issueId);
    long generateIssueId();
    long generateIssueId(int projectId);
}
