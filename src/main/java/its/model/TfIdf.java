package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * model for TF-IDF
 * vocabulary, TF-IDFs, TFs, IDF, TF-IDF, TF, helper
 *
 * @author hanung
 */

public class TfIdf {
    
    // for cut
    private static final int MIN_WORD_COUNT = 3;
    // for weight
    private static final int titleWeight = 3;
    private static final int descriptionWeight = 2;
    private static final int commentWeight = 1;
    private static final int generalWeight = 1;
    // for vocabulary
    private Set<String> vocabulary = new HashSet<>();
    // for IDF
    private Map<String, Double> idf = new HashMap<>();

    // build vocabulary
    public void buildVocabulary(List<Issue> issues) {
        if (issues == null) {
            return;
        }

        for (Issue issue : issues) {
            vocabulary.addAll(tokenize(issue.getTitle()));
            vocabulary.addAll(tokenize(issue.getDescription()));

            for (Comment comment : issue.getComments()) {
                if (comment == null) {
                    continue;
                }

                vocabulary.addAll(tokenize(comment.getContent()));
            }
        }
    }

    // TF-IDFs
    public Map<Long, Map<String, Double>> calculateTfIdfByIssues(List<Issue> issues) {
        Map<Long, Map<String, Double>> result = new HashMap<>();

        if (issues == null || issues.isEmpty()) {
            return result;
        }

        buildVocabulary(issues);
        calculateIdfByDocument(issues);

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            Map<String, Double> tfIdfVector =
                    calculateTfIdfByIssue(issue, this.vocabulary, this.idf);

            result.put(issue.getIssueId(), tfIdfVector);
        }

        return result;
    }

    // TFs
    public Map<Long, Map<String, Double>> calculateTfByIssues(List<Issue> issues) {
        Map<Long, Map<String, Double>> result = new HashMap<>();

        if (issues == null || issues.isEmpty()) {
            return result;
        }

        buildVocabulary(issues);

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            Map<String, Double> tfVector = calculateTfByIssue(issue, this.vocabulary);
            result.put(issue.getIssueId(), tfVector);
        }

        return result;
    }

    // IDF
    public Map<String, Double> calculateIdfByDocument(List<Issue> issues) {
        // {"word1": idf, "word2": idf, ...}
        Map<String, Double> idfByDocument = new HashMap<>();

        if (issues == null || issues.isEmpty()) {
            for (String word : vocabulary) {
                idfByDocument.put(word, 1.0);
            }
            return idfByDocument;
        }

        // count
        //{document1:{"word1":count, "word2":count, ...}, document2:{"word1":count, "word2":count, ...}, ...}
        Map<Integer, Map<String, Integer>> documentWordCounts = countWordsByDocument(issues);

        // cut
        for (Map<String, Integer> wordCount : documentWordCounts.values()) {
            cutWords(wordCount, MIN_WORD_COUNT);
        }

        // total document
        int totalDocumentCount = documentWordCounts.size();

        // document frequency
        // {"word1":count, "word2":count, ...}
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Map<String, Integer> wordCount : documentWordCounts.values()) {
            for (String word : wordCount.keySet()) {
                documentFrequency.put(
                        word,
                        documentFrequency.getOrDefault(word, 0) + 1
                );
            }
        }

        // calculate IDF with mapping onto vocabulary
        for (String word : vocabulary) {
            int df = documentFrequency.getOrDefault(word, 0);

            // Smoothing
            double idfValue = Math.log((totalDocumentCount + 1.0) / (df + 1.0)) + 1.0;

            idfByDocument.put(word, idfValue);
        }

        this.idf = idfByDocument;

        return idfByDocument;
    }
    
    // TF-IDF
    public Map<String, Double> calculateTfIdfByIssue(
            Issue issue, Set<String> vocabulary, Map<String, Double> idfMap) {

        Map<String, Double> tfIdfVector = new HashMap<>();

        if (issue == null || vocabulary == null || vocabulary.isEmpty()) {
            return tfIdfVector;
        }

        if (idfMap == null) {
            idfMap = new HashMap<>();
        }

        Map<String, Double> tfVector = calculateTfByIssue(issue, vocabulary);

        for (String word : vocabulary) {
            if (word == null) {
                continue;
            }

            double tf = tfVector.getOrDefault(word, 0.0);
            double idf = idfMap.getOrDefault(word, 1.0);

            tfIdfVector.put(word, tf * idf);
        }

        return tfIdfVector;
    }

    // TF
    public Map<String, Double> calculateTfByIssue(Issue issue, Set<String> vocabulary) {
        Map<String, Double> tfVector = new HashMap<>();

        if (issue == null || vocabulary == null || vocabulary.isEmpty()) {
            return tfVector;
        }

        Map<String, Integer> wordCounts = countWordsByIssue(issue);
        cutWords(wordCounts, 3);

        int totalWeightCount = 0;
        for (Integer count : wordCounts.values()) {
            if (count != null) {
                totalWeightCount += count;
            }
        }

        for (String word : vocabulary) {
            if (word == null) {
                continue;
            }

            double tf = 0.0;
            if (totalWeightCount > 0 && wordCounts.containsKey(word)) {
                tf = (double) wordCounts.get(word) / totalWeightCount;
            }

            tfVector.put(word, tf);
        }

        return tfVector;
    }

    // count for tf
    public Map<String, Integer> countWordsByIssue(Issue issue) {
        // {"word1":count, "word2":count, ...}
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
    private Map<Integer, Map<String, Integer>> countWordsByDocument(List<Issue> issues) {
        // {categoryId1:{"word1": count, "word2": count, ...}, categoryId2:{"word1": count, ...}}
        Map<Integer, Map<String, Integer>> documentWordCounts = new HashMap<>();
        
        // for uncategorized issue
        int virtualCategory = -1;

        for (Issue issue : issues) {
            if (issue == null) {
                continue;
            }

            int targetKey = issue.getCategoryId();

            // regrad uncategorized issue as separate document
            if (targetKey <= 0) {
                targetKey = virtualCategory;
                virtualCategory--; 
            }

            // documentWordCounts.value를 변수 wordCount로 관리, 있으면 그대로 사용, 없으면 생성
            Map<String, Integer> wordCount = documentWordCounts.computeIfAbsent(
                    targetKey,
                    key -> new HashMap<>()
            );

            // wordCount에 generalWeight 사용
            addText(wordCount, issue.getTitle(), generalWeight);
            addText(wordCount, issue.getDescription(), generalWeight);

            if (issue.getComments() != null) {
                for (Comment comment : issue.getComments()) {
                    if (comment == null) {
                        continue;
                    }
                    addText(wordCount, comment.getContent(), generalWeight);
                }
            }
        }

        return documentWordCounts;
    }

    // add Text with weight
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

    // tokenizer
    private List<String> tokenize(String text) {
        // ["word1", "word2", "word3", ...]
        List<String> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        // lowercase
        String normalizedText = text.toLowerCase();
        // alphabet and number only
        normalizedText = normalizedText.replaceAll("[^a-z0-9]", " ");
        // split by space
        String[] tokens = normalizedText.split("\\s+");

        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            
            result.add(token);
        }

        return result;
    }

    // cut
    public void cutWords(Map<String, Integer> wordCount, int minCount) {
        if (wordCount == null) {
            return;
        }

        wordCount.entrySet().removeIf(entry -> entry.getValue() < minCount);
    }

    // get
    public Set<String> getVocabulary() {
        return vocabulary;
    }

    public Map<String, Double> getIdf() {
        return idf;
    }

    // cosine similarity
    public double cosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()){
            return 0.0;
        }

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(vectorA.keySet());
        vocabulary.addAll(vectorB.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : vocabulary) {
            if (word == null) {
                continue;
            }
            double valA = vectorA.getOrDefault(word, 0.0);
            double valB = vectorB.getOrDefault(word, 0.0);

            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
