package its.repository;

import java.util.List;

import its.model.Issue;

public interface IssueRepository {

    void save(Issue issue);
    Issue findById(int issueId);
    Issue findByProjectIdAndIssueId(int projectId, int issueId);
    List<Issue> findAll();
    List<Issue> findByProjectId(int projectId);

    void update(Issue issue);
    void delete(int issueId);
    void delete(int projectId, int issueId);
    int generateIssueId();
    int generateIssueId(int projectId);
}
