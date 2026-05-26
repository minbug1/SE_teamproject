package its.view.console;

import its.controller.RecommendationController;
import its.model.Issue;
import its.model.IssueSimilarity;
import its.model.TFIDF;
import its.repository.FileIssueRepository;
import its.repository.FileUserRepository;
import its.repository.IssueRepository;
import its.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RecommendationConsoleUI {

    private final RecommendationController recommendationController;
    private final IssueRepository issueRepository;
    private final TFIDF tfIdfAnalyzer;
    private final IssueSimilarity issueSimilarity;
    private final Scanner scanner;

    public RecommendationConsoleUI(
            RecommendationController recommendationController,
            IssueRepository issueRepository
    ) {
        if (recommendationController == null) {
            throw new IllegalArgumentException("Recommendation controller must not be null.");
        }

        if (issueRepository == null) {
            throw new IllegalArgumentException("Issue repository must not be null.");
        }

        this.recommendationController = recommendationController;
        this.issueRepository = issueRepository;
        this.tfIdfAnalyzer = new TFIDF();
        this.issueSimilarity = new IssueSimilarity();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        while (true) {
            printMenu();

            int menu = readInt("Select menu: ");

            if (menu == 0) {
                System.out.println("Exit recommendation console.");
                break;
            }

            switch (menu) {
                case 1:
                    recommendByIssueId();
                    break;

                case 2:
                    previewInitialCategorize();
                    break;

                case 3:
                    showIssueWordSet();
                    break;

                case 4:
                    showJaccardSimilarityByIssueId();
                    break;

                default:
                    System.out.println("Invalid menu. Try again.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("==================================");
        System.out.println(" Developer Recommendation Console ");
        System.out.println("==================================");
        System.out.println("1. Recommend developers by issue ID");
        System.out.println("2. Preview initial categorize");
        System.out.println("3. Show issue word set");
        System.out.println("4. Show Jaccard similarity by issue ID");
        System.out.println("0. Exit");
        System.out.println("==================================");
    }

    /*
     * Menu 1.
     * issueId를 입력받아 추천 developer 목록을 출력한다.
     */
    private void recommendByIssueId() {
        long issueId = readLong("Enter issue ID: ");
        int topN = readInt("Enter top N: ");

        try {
            List<RecommendationController.DeveloperRecommendation> recommendations =
                    recommendationController.recommendDevelopers(issueId, topN);

            printRecommendations(issueId, recommendations);

        } catch (IllegalArgumentException e) {
            System.out.println("Recommendation failed: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    private void printRecommendations(
            long issueId,
            List<RecommendationController.DeveloperRecommendation> recommendations
    ) {
        System.out.println();
        System.out.println("Recommendation result for Issue #" + issueId);
        System.out.println("----------------------------------");

        if (recommendations == null || recommendations.isEmpty()) {
            System.out.println("No developer recommendation found.");
            return;
        }

        int rank = 1;

        for (RecommendationController.DeveloperRecommendation recommendation : recommendations) {
            System.out.println("Rank " + rank);
            System.out.println("Developer ID      : "
                    + recommendation.getDeveloper().getUserId());
            System.out.println("Developer Login ID: "
                    + recommendation.getDeveloper().getLoginId());
            System.out.println("Final Score       : "
                    + formatScore(recommendation.getFinalScore()));
            System.out.println("Similarity Score  : "
                    + formatScore(recommendation.getSimilarityScore()));
            System.out.println("Category Score    : "
                    + formatScore(recommendation.getCategoryScore()));
            System.out.println("Priority Score    : "
                    + formatScore(recommendation.getPriorityScore()));
            System.out.println("Experience Score  : "
                    + formatScore(recommendation.getExperienceScore()));
            System.out.println("Workload Penalty  : "
                    + formatScore(recommendation.getWorkloadPenalty()));
            System.out.println("Closed Count      : "
                    + recommendation.getStatistics().getClosedCount());
            System.out.println("Current Assigned  : "
                    + recommendation.getStatistics().getCurrentAssignedCount());
            System.out.println("----------------------------------");

            rank++;
        }
    }

    /*
     * Menu 2.
     * TF 기반 word set을 만들고,
     * Jaccard similarity threshold 이상인 issue들을 cluster로 묶어 보여준다.
     *
     * 실제 categoryId를 변경하지 않는다.
     * 내부 categorize가 어떻게 진행될지 확인하는 preview 기능이다.
     */
    private void previewInitialCategorize() {
        List<Issue> issues = issueRepository.findAll();

        Map<Long, HashSet<String>> wordSetByIssue =
                tfIdfAnalyzer.buildWordSetByIssue(issues);

        double threshold = readDouble("Enter Jaccard threshold, example 0.25: ");

        ArrayList<ArrayList<Long>> clusters =
                buildClustersByJaccard(wordSetByIssue, threshold);

        printClusters(clusters, wordSetByIssue, issues);
    }

    /*
     * Jaccard threshold 기반으로 issue graph를 만들고,
     * 연결 요소 connected component를 cluster로 본다.
     *
     * issue = node
     * similarity >= threshold = edge
     * cluster = connected component
     */
    private ArrayList<ArrayList<Long>> buildClustersByJaccard(
            Map<Long, HashSet<String>> wordSetByIssue,
            double threshold
    ) {
        ArrayList<ArrayList<Long>> clusters = new ArrayList<>();

        if (wordSetByIssue == null || wordSetByIssue.isEmpty()) {
            return clusters;
        }

        ArrayList<Long> issueIds = new ArrayList<>(wordSetByIssue.keySet());
        HashSet<Long> visited = new HashSet<>();

        for (Long startIssueId : issueIds) {
            if (startIssueId == null || visited.contains(startIssueId)) {
                continue;
            }

            ArrayList<Long> cluster = new ArrayList<>();
            ArrayList<Long> queue = new ArrayList<>();

            queue.add(startIssueId);
            visited.add(startIssueId);

            int index = 0;

            while (index < queue.size()) {
                Long currentIssueId = queue.get(index);
                index++;

                cluster.add(currentIssueId);

                HashSet<String> currentWords = wordSetByIssue.get(currentIssueId);

                for (Long otherIssueId : issueIds) {
                    if (otherIssueId == null || visited.contains(otherIssueId)) {
                        continue;
                    }

                    HashSet<String> otherWords = wordSetByIssue.get(otherIssueId);

                    double similarity =
                            issueSimilarity.calculateJaccardSimilarity(currentWords, otherWords);

                    if (similarity >= threshold) {
                        visited.add(otherIssueId);
                        queue.add(otherIssueId);
                    }
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    private void printClusters(
            ArrayList<ArrayList<Long>> clusters,
            Map<Long, HashSet<String>> wordSetByIssue,
            List<Issue> issues
    ) {
        System.out.println();
        System.out.println("===== Initial Categorize Preview =====");

        if (clusters == null || clusters.isEmpty()) {
            System.out.println("No cluster found.");
            return;
        }

        System.out.println("Cluster count: " + clusters.size());

        int clusterIndex = 1;

        for (ArrayList<Long> cluster : clusters) {
            System.out.println();
            System.out.println("Cluster #" + clusterIndex);
            System.out.println("----------------------------------");

            HashSet<String> clusterWords = new HashSet<>();

            for (Long issueId : cluster) {
                Issue issue = findIssueById(issues, issueId);

                if (issue == null) {
                    continue;
                }

                HashSet<String> words = wordSetByIssue.get(issueId);

                if (words != null) {
                    clusterWords.addAll(words);
                }

                System.out.println("Issue #" + issue.getIssueId()
                        + " | " + issue.getTitle()
                        + " | saved categoryId=" + issue.getCategoryId()
                        + " | status=" + issue.getStatus());
            }

            System.out.println("Representative words: " + clusterWords);
            System.out.println("----------------------------------");

            clusterIndex++;
        }
    }

    /*
     * Menu 3.
     * 각 issue가 TF 기준으로 어떤 단어 집합을 가지는지 출력한다.
     *
     * title 3회, description 2회, comment 1회 반영 후
     * 3회 이상 등장한 단어만 남는다.
     */
    private void showIssueWordSet() {
        List<Issue> issues = issueRepository.findAll();

        Map<Long, HashSet<String>> wordSetByIssue =
                tfIdfAnalyzer.buildWordSetByIssue(issues);

        System.out.println();
        System.out.println("===== Issue Word Set Preview =====");

        if (issues == null || issues.isEmpty()) {
            System.out.println("No issue found.");
            return;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            HashSet<String> words = wordSetByIssue.get(issue.getIssueId());

            System.out.println();
            System.out.println("Issue #" + issue.getIssueId()
                    + " | " + issue.getTitle()
                    + " | categoryId=" + issue.getCategoryId()
                    + " | status=" + issue.getStatus());

            if (words == null || words.isEmpty()) {
                System.out.println("Words: []");
            } else {
                System.out.println("Words: " + words);
            }
        }
    }

    /*
     * Menu 4.
     * 특정 issue를 기준으로 다른 issue들과의 Jaccard similarity를 출력한다.
     *
     * 초기 categorize가 왜 특정 issue들을 묶는지 확인할 때 사용한다.
     */
    private void showJaccardSimilarityByIssueId() {
        long targetIssueId = readLong("Enter target issue ID: ");

        List<Issue> issues = issueRepository.findAll();

        Map<Long, HashSet<String>> wordSetByIssue =
                tfIdfAnalyzer.buildWordSetByIssue(issues);

        HashSet<String> targetWords = wordSetByIssue.get(targetIssueId);

        if (targetWords == null || targetWords.isEmpty()) {
            System.out.println("Target issue has no valid word set.");
            return;
        }

        System.out.println();
        System.out.println("===== Jaccard Similarity by Issue #" + targetIssueId + " =====");
        System.out.println("Target words: " + targetWords);
        System.out.println("----------------------------------");

        ArrayList<JaccardResult> results = new ArrayList<>();

        for (Map.Entry<Long, HashSet<String>> entry : wordSetByIssue.entrySet()) {
            Long otherIssueId = entry.getKey();

            if (otherIssueId == null || otherIssueId == targetIssueId) {
                continue;
            }

            HashSet<String> otherWords = entry.getValue();

            double similarity =
                    issueSimilarity.calculateJaccardSimilarity(targetWords, otherWords);

            results.add(new JaccardResult(otherIssueId, similarity));
        }

        sortJaccardResults(results);

        for (JaccardResult result : results) {
            Issue issue = findIssueById(issues, result.getIssueId());

            if (issue == null) {
                continue;
            }

            System.out.println("Issue #" + issue.getIssueId()
                    + " | " + issue.getTitle()
                    + " | categoryId=" + issue.getCategoryId()
                    + " | similarity=" + formatScore(result.getSimilarity()));
        }
    }

    private void sortJaccardResults(ArrayList<JaccardResult> results) {
        results.sort((left, right) ->
                Double.compare(right.getSimilarity(), left.getSimilarity())
        );
    }

    private Issue findIssueById(List<Issue> issues, long issueId) {
        if (issues == null || issueId <= 0) {
            return null;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getIssueId() == issueId) {
                return issue;
            }
        }

        return null;
    }

    private String formatScore(double value) {
        return String.format("%.4f", value);
    }

    private int readInt(String message) {
        while (true) {
            System.out.print(message);

            try {
                String input = scanner.nextLine();
                return Integer.parseInt(input.trim());

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private long readLong(String message) {
        while (true) {
            System.out.print(message);

            try {
                String input = scanner.nextLine();
                return Long.parseLong(input.trim());

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private double readDouble(String message) {
        while (true) {
            System.out.print(message);

            try {
                String input = scanner.nextLine();
                return Double.parseDouble(input.trim());

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }

    private static class JaccardResult {

        private final long issueId;
        private final double similarity;

        public JaccardResult(long issueId, double similarity) {
            this.issueId = issueId;
            this.similarity = similarity;
        }

        public long getIssueId() {
            return issueId;
        }

        public double getSimilarity() {
            return similarity;
        }
    }

    public static void main(String[] args) {
        UserRepository userRepository = new FileUserRepository();
        IssueRepository issueRepository = new FileIssueRepository(userRepository);

        RecommendationController recommendationController =
                new RecommendationController(issueRepository, userRepository);

        RecommendationConsoleUI ui =
                new RecommendationConsoleUI(recommendationController, issueRepository);

        ui.start();
    }
}