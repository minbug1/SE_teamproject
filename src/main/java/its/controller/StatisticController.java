package its.controller;

import its.model.Issue;
import its.model.IssueStatistics;
import its.model.StatisticsEngine;
import its.repository.FileIssueRepository;
import its.repository.IssueRepository;

import java.util.List;

/**
 * Controller for statistics.
 *
 * 책임:
 * - issueRepository에서 issue 목록을 가져온다.
 * - StatisticCalculator에 계산을 위임한다.
 * - UI에 IssueStatistics DTO를 반환한다.
 *
 * 하지 않는 일:
 * - 출력 형식 결정
 * - 날짜순 정렬
 * - 표 생성
 * - 문자열 포맷팅
 * - category 자동 분류/병합/분할
 */
public class StatisticController {

    private final IssueRepository issueRepository;
    private final StatisticsEngine statisticCalculator;

    public StatisticController() {
        this(new FileIssueRepository(), new StatisticsEngine());
    }

    public StatisticController(
            IssueRepository issueRepository,
            StatisticsEngine statisticCalculator
    ) {
        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }

        if (statisticCalculator == null) {
            throw new IllegalArgumentException("Statistic calculator must not be null.");
        }

        this.issueRepository = issueRepository;
        this.statisticCalculator = statisticCalculator;
    }

    /**
     * 전체 issue 통계 조회.
     */
    public IssueStatistics getIssueStatistics() {
        List<Issue> issues = issueRepository.findAll();
        return statisticCalculator.calculate(issues);
    }

    /**
     * 특정 project의 issue 통계 조회.
     */
    public IssueStatistics getIssueStatisticsByProject(int projectId) {
        List<Issue> issues = issueRepository.findByProjectId(projectId);
        return statisticCalculator.calculate(issues);
    }
}