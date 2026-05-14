package its.repository;

import java.util.List;

import its.model.Issue;

public class FileIssueRepository implements IssueRepository {

    private List<Issue> issues; 

    @Override
    public void save(Issue issue) {
        issues.add(issue);
    }

    @Override
    public Issue findById(int issueId) {
        return issues.stream()
                .filter(i -> i.getIssueId() == issueId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Issue> findAll() {
        // 파일에서 모든 이슈 조회 로직 구현
        return null;
    }

    @Override
    public void update(Issue issue) {
        // 파일에 이슈 업데이트 로직 구현
    }

    @Override
    public void delete(int issueId) {
        // 파일에서 이슈 삭제 로직 구현
    }

}
