package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TFIDF{

    // for cut
    private static final int MIN_WORD_COUNT = 3;
    // for weight
    private static final int titleWeight = 3;
    private static final int descriptionWeight = 2;
    private static final int commentWeight = 1;

    // TF
    public Map<Long, Map<String, Double>> calculateTfByIssue(List<Issue> issues) {
        // {issueId: {"word": tf}}
        Map<Long, Map<String, Double>> tfByIssue = new HashMap<>();

        if (issues == null) {
            return tfByIssue;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            // count
            Map<String, Integer> issueWordCounts = countWordsForTf(issue);

            // cut
            cutWords(issueWordCounts, MIN_WORD_COUNT);

            // TF
            Map<String, Double> tf = convertCountsToTf(issueWordCounts);

            tfByIssue.put(issue.getIssueId(), tf);
        }

        return tfByIssue;
    }

    // IDF
    public Map<String, Double> calculateIdfByCategory(List<Issue> issues) {
        // {category_id: {"word": count}}
        Map<Integer, Map<String, Integer>> categoryWordCounts = countWordsForIdf(issues);

        // cut
        for (Map<String, Integer> wordCount : categoryWordCounts.values()) {
            cutWords(wordCount, MIN_WORD_COUNT);
        }

        // category 수
        int categoryDocumentCount = categoryWordCounts.size();

        Map<String, Integer> documentFrequency = new HashMap<>();
        // category 별 unique words
        for (Map<String, Integer> wordCount : categoryWordCounts.values()) {
            // 전체 category의 unique words frequency
            for (String word : wordCount.keySet()) {
                documentFrequency.put(
                        word,
                        documentFrequency.getOrDefault(word, 0) + 1
                );
            }
        }

        Map<String, Double> idfByDocument = new HashMap<>();

        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            String word = entry.getKey();
            int df = entry.getValue();
            // 0 방지
            double idfValue =
                    Math.log((categoryDocumentCount + 1.0) / (df + 1.0)) + 1.0;

            idfByDocument.put(word, idfValue);
        }

        return idfByDocument;
    }

    // TF-IDF
    public Map<Long, Map<String, Double>> calculateTfIdfByIssue(List<Issue> issues) {
        Map<Long, Map<String, Double>> tfByIssue = calculateTfByIssue(issues);
        Map<String, Double> idfByDocument = calculateIdfByCategory(issues);
        // {issueId: {"word": tf-idf}}
        Map<Long, Map<String, Double>> tfIdfByIssue = new HashMap<>();

        for (Map.Entry<Long, Map<String, Double>> issueEntry : tfByIssue.entrySet()) {
            long issueId = issueEntry.getKey();
            Map<String, Double> tf = issueEntry.getValue();

            Map<String, Double> tfIdf = new HashMap<>();

            for (Map.Entry<String, Double> wordEntry : tf.entrySet()) {
                String word = wordEntry.getKey();
                double tfValue = wordEntry.getValue();

                /*
                 * IDF 문서에 없는 단어는 category 기준으로 의미가 확정되지 않은 단어이므로 제외.
                 */
                if (!idfByDocument.containsKey(word)) {
                    continue;
                }

                double idfValue = idfByDocument.get(word);
                tfIdf.put(word, tfValue * idfValue);
            }

            tfIdfByIssue.put(issueId, tfIdf);
        }

        return tfIdfByIssue;
    }

    // for categorization
    public Map<Long, HashSet<String>> buildWordSetByIssue(List<Issue> issues) {
        // {issueId: {word set}}
        Map<Long, HashSet<String>> wordSetByIssue = new HashMap<>();

        if (issues == null) {
            return wordSetByIssue;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }
            // count
            Map<String, Integer> wordCount = countWordsForTf(issue);
            // cut
            cutWords(wordCount, MIN_WORD_COUNT);
            // word set
            wordSetByIssue.put(
                    issue.getIssueId(),
                    new HashSet<>(wordCount.keySet())
            );
        }

        return wordSetByIssue;
    }

    // count for tf
    private Map<String, Integer> countWordsForTf(Issue issue) {
        // {"word":count}
        Map<String, Integer> wordCount = new HashMap<>();

        if (issue == null) {
            return wordCount;
        }

        addText(wordCount, issue.getTitle(), titleWeight);
        addText(wordCount, issue.getDescription(), descriptionWeight);

        for (Comment comment : issue.getComments()) {
            if (comment == null) {
                continue;
            }

            addText(wordCount, comment.getContent(), commentWeight);
        }

        return wordCount;
    }

    // count for idf
    private Map<Integer, Map<String, Integer>> countWordsForIdf(List<Issue> issues) {
        // {category_id:{"word":count}}
        Map<Integer, Map<String, Integer>> categoryWordCounts = new HashMap<>();

        if (issues == null) {
            return categoryWordCounts;
        }

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            int categoryId = issue.getCategoryId();

            // 미분류
            if (categoryId <= 0) {
                continue;
            }
            // category 없으면, 만들어서 추가
            Map<String, Integer> wordCount =
                    categoryWordCounts.computeIfAbsent(
                            categoryId,
                            key -> new HashMap<>()
                    );

            addText(wordCount, issue.getTitle(), 1);
            addText(wordCount, issue.getDescription(), 1);

            for (Comment comment : issue.getComments()) {
                if (comment == null) {
                    continue;
                }

                addText(wordCount, comment.getContent(), 1);
            }
        }

        return categoryWordCounts;
    }

    // weight
    private void addText(Map<String, Integer> wordCount, String text, int weight) {
        if (wordCount == null) {
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        if (weight <= 0) {
            return;
        }
        // tokenize
        List<String> words = tokenize(text);
        // add weighted count
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + weight);
        }
    }

    // tokenize
    private List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();

        if (text == null) {
            return result;
        }

        // lowercase
        String normalized = text.toLowerCase();
        // alphabet, number only
        normalized = normalized.replaceAll("[^a-z0-9]", " ");
        // split by whitespace
        String[] tokens = normalized.split("\\s+");

        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            // filter
            if (filterWord(token)) {
                continue;
            }

            result.add(token);
        }

        return result;
    }

    // filter
    private boolean filterWord(String word) {
        return word.equals("issue");
    }

    // cut
    private void cutWords(Map<String, Integer> wordCount, int minCount) {
        if (wordCount == null) {
            return;
        }

        wordCount.entrySet().removeIf(entry -> entry.getValue() < minCount);
    }

    // TF
    private Map<String, Double> convertCountsToTf(Map<String, Integer> wordCount) {
        Map<String, Double> tf = new HashMap<>();

        if (wordCount == null || wordCount.isEmpty()) {
            return tf;
        }

        int totalCount = 0;

        for (int count : wordCount.values()) {
            totalCount += count;
        }

        if (totalCount <= 0) {
            return tf;
        }

        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();

            tf.put(word, (double) count / totalCount);
        }

        return tf;
    }
}