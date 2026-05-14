package its.repository;

import java.util.List;

import its.model.Issue;

public interface IssueRepository {
    void save(Issue issue);
    Issue findById(int issueId);
    List<Issue> findAll();
    void update(Issue issue);
    void delete(int issueId);
}


