package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Category {

    private int categoryId;
    private String name;
    private HashSet<String> representativeWords;

    public Category(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
        this.representativeWords = new HashSet<>();
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public HashSet<String> getRepresentativeWords() {
        return representativeWords;
    }

    public void addWords(HashSet<String> words) {
        if (words != null) {
            representativeWords.addAll(words);
        }
    }

    /*
     * categoryId -> Category 형태로 현재 category 현황을 만든다.
     * 통계 화면에서
     * 1 : [login, password, auth]
     * 2 : [button, screen, ui]
     * 이런 식으로 보여주기 위한 용도.
     */
    public static Map<Integer, Category> analyzeCategories(List<Issue> issues) {
        Map<Integer, Category> categories = new HashMap<>();

        if (issues == null) {
            return categories;
        }

        TFIDF tfidf = new TFIDF();
        Map<Long, HashSet<String>> wordSetByIssue = tfidf.buildWordSetByIssue(issues);

        for (Issue issue : issues) {
            if (issue == null || issue.getCategoryId() == 0) {
                continue;
            }

            int categoryId = issue.getCategoryId();
            String categoryName = issue.getCategoryName();

            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = "Category " + categoryId;
            }

            Category category = categories.get(categoryId);

            if (category == null) {
                category = new Category(categoryId, categoryName);
                categories.put(categoryId, category);
            }

            category.addWords(wordSetByIssue.get(issue.getIssueId()));
        }

        return categories;
    }

    /*
     * category 이름 변경.
     * 같은 categoryId를 가진 issue들의 categoryName만 바꾼다.
     */
    public static List<Issue> renameCategory(
            List<Issue> issues,
            int categoryId,
            String newName
    ) {
        List<Issue> updatedIssues = new ArrayList<>();

        if (issues == null || categoryId <= 0 ||
                newName == null || newName.trim().isEmpty()) {
            return updatedIssues;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (issue.getCategoryId() == categoryId) {
                issue.setCategoryName(newName.trim());
                updatedIssues.add(issue);
            }
        }

        return updatedIssues;
    }

    /*
     * category 병합.
     * sourceCategoryIds에 속한 issue들을 targetCategoryId/name으로 바꾼다.
     */
    public static List<Issue> mergeCategories(
            List<Issue> issues,
            List<Integer> sourceCategoryIds,
            int targetCategoryId,
            String targetCategoryName
    ) {
        List<Issue> updatedIssues = new ArrayList<>();

        if (issues == null || sourceCategoryIds == null ||
                sourceCategoryIds.isEmpty() || targetCategoryId <= 0) {
            return updatedIssues;
        }

        if (targetCategoryName == null || targetCategoryName.trim().isEmpty()) {
            targetCategoryName = "Category " + targetCategoryId;
        }

        HashSet<Integer> mergeIds = new HashSet<>(sourceCategoryIds);
        mergeIds.add(targetCategoryId);

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            if (mergeIds.contains(issue.getCategoryId())) {
                issue.setCategoryId(targetCategoryId);
                issue.setCategoryName(targetCategoryName.trim());
                updatedIssues.add(issue);
            }
        }

        return updatedIssues;
    }

    /*
     * categoryId == 0인 issue만 자동 배정.
     * 기존 category와 Jaccard가 threshold 이상이면 기존 category,
     * 아니면 새 category를 만든다.
     */
    public static List<Issue> assignUncategorizedIssues(
            List<Issue> issues,
            double threshold
    ) {
        List<Issue> updatedIssues = new ArrayList<>();

        if (issues == null || threshold < 0.0 || threshold > 1.0) {
            return updatedIssues;
        }

        TFIDF tfidf = new TFIDF();
        IssueSimilarity similarity = new IssueSimilarity();

        Map<Long, HashSet<String>> wordSetByIssue = tfidf.buildWordSetByIssue(issues);
        Map<Integer, Category> categories = analyzeCategories(issues);

        int nextCategoryId = findNextCategoryId(issues);

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            // 이미 category가 있으면 절대 건드리지 않음
            if (issue.getCategoryId() != 0) {
                continue;
            }

            HashSet<String> issueWords = wordSetByIssue.get(issue.getIssueId());

            int bestCategoryId = 0;
            String bestCategoryName = null;
            double bestScore = 0.0;

            for (Category category : categories.values()) {
                double score = similarity.calculateJaccardSimilarity(
                        issueWords,
                        category.getRepresentativeWords()
                );

                if (score > bestScore) {
                    bestScore = score;
                    bestCategoryId = category.getCategoryId();
                    bestCategoryName = category.getName();
                }
            }

            if (bestCategoryId != 0 && bestScore >= threshold) {
                issue.setCategoryId(bestCategoryId);
                issue.setCategoryName(bestCategoryName);
            } else {
                issue.setCategoryId(nextCategoryId);
                issue.setCategoryName("Category " + nextCategoryId);

                Category newCategory = new Category(
                        nextCategoryId,
                        "Category " + nextCategoryId
                );
                newCategory.addWords(issueWords);
                categories.put(nextCategoryId, newCategory);

                nextCategoryId++;
            }

            Category assignedCategory = categories.get(issue.getCategoryId());
            if (assignedCategory != null) {
                assignedCategory.addWords(issueWords);
            }

            updatedIssues.add(issue);
        }

        return updatedIssues;
    }

    private static int findNextCategoryId(List<Issue> issues) {
        int max = 0;

        if (issues == null) {
            return 1;
        }

        for (Issue issue : issues) {
            if (issue != null && issue.getCategoryId() > max) {
                max = issue.getCategoryId();
            }
        }

        return max + 1;
    }
}