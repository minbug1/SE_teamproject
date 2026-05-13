package its.repository;

import java.util.List;

import its.model.Issue;

public class FileIssueRepository implements IssueRepository {

    @Override
    public void save(Issue issue) {
        // 파일에 이슈 저장 로직 구현
    }

    @Override
    public Issue findById(Long issueId) {
        // 파일에서 이슈 조회 로직 구현
        return null;
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
    public void delete(Long issueId) {
        // 파일에서 이슈 삭제 로직 구현
    }

}
