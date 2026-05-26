package its.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class IssueSimilarity {

    /*
     * Jaccard Similarity
     *
     * 초기 categorize에서 사용한다.
     *
     * 두 issue의 단어 집합이 얼마나 겹치는지를 계산한다.
     *
     * Jaccard = 교집합 크기 / 합집합 크기
     */
    public double calculateJaccardSimilarity(HashSet<String> left, HashSet<String> right) {
        if (left == null || right == null) {
            return 0.0;
        }

        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        HashSet<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);

        HashSet<String> union = new HashSet<>(left);
        union.addAll(right);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /*
     * Cosine Similarity
     *
     * TF-IDF vector 기반 issue similarity 계산에 사용한다.
     *
     * Cosine = dot(A, B) / (|A| * |B|)
     *
     * Map<String, Double>은 다음과 같은 벡터를 의미한다.
     *
     * {
     *   "login": 0.31,
     *   "session": 0.22,
     *   "redirect": 0.18
     * }
     */
    public double calculateCosineSimilarity(
            Map<String, Double> leftVector,
            Map<String, Double> rightVector
    ) {
        if (leftVector == null || rightVector == null) {
            return 0.0;
        }

        if (leftVector.isEmpty() || rightVector.isEmpty()) {
            return 0.0;
        }

        double dotProduct = calculateDotProduct(leftVector, rightVector);
        double leftMagnitude = calculateMagnitude(leftVector);
        double rightMagnitude = calculateMagnitude(rightVector);

        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
            return 0.0;
        }

        return dotProduct / (leftMagnitude * rightMagnitude);
    }

    /*
     * 두 벡터의 내적 계산.
     *
     * 효율을 위해 더 작은 map을 기준으로 순회한다.
     */
    private double calculateDotProduct(
            Map<String, Double> leftVector,
            Map<String, Double> rightVector
    ) {
        Map<String, Double> smaller = leftVector;
        Map<String, Double> larger = rightVector;

        if (leftVector.size() > rightVector.size()) {
            smaller = rightVector;
            larger = leftVector;
        }

        double dotProduct = 0.0;

        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            String word = entry.getKey();
            Double leftValue = entry.getValue();

            if (leftValue == null) {
                continue;
            }

            Double rightValue = larger.get(word);

            if (rightValue == null) {
                continue;
            }

            dotProduct += leftValue * rightValue;
        }

        return dotProduct;
    }

    /*
     * 벡터 크기 계산.
     *
     * magnitude = sqrt(v1^2 + v2^2 + ... + vn^2)
     */
    private double calculateMagnitude(Map<String, Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;

        for (Double value : vector.values()) {
            if (value == null) {
                continue;
            }

            sum += value * value;
        }

        return Math.sqrt(sum);
    }

    /*
     * 특정 issue와 다른 issue들의 cosine similarity 계산.
     *
     * targetIssueId를 기준으로 모든 issue의 유사도를 구한다.
     *
     * 입력:
     * issueId -> TF-IDF vector
     *
     * 출력:
     * otherIssueId -> similarity
     */
    public Map<Long, Double> calculateCosineSimilarityByIssue(
            long targetIssueId,
            Map<Long, Map<String, Double>> tfIdfByIssue
    ) {
        Map<Long, Double> result = new HashMap<>();

        if (targetIssueId <= 0 || tfIdfByIssue == null || tfIdfByIssue.isEmpty()) {
            return result;
        }

        Map<String, Double> targetVector = tfIdfByIssue.get(targetIssueId);

        if (targetVector == null || targetVector.isEmpty()) {
            return result;
        }

        for (Map.Entry<Long, Map<String, Double>> entry : tfIdfByIssue.entrySet()) {
            long issueId = entry.getKey();

            if (issueId == targetIssueId) {
                continue;
            }

            Map<String, Double> otherVector = entry.getValue();

            double similarity = calculateCosineSimilarity(targetVector, otherVector);
            result.put(issueId, similarity);
        }

        return result;
    }

    /*
     * 특정 issue와 다른 issue들의 Jaccard similarity 계산.
     *
     * 초기 categorize 단계에서 사용할 수 있다.
     *
     * 입력:
     * issueId -> word set
     *
     * 출력:
     * otherIssueId -> similarity
     */
    public Map<Long, Double> calculateJaccardSimilarityByIssue(
            long targetIssueId,
            Map<Long, HashSet<String>> wordSetByIssue
    ) {
        Map<Long, Double> result = new HashMap<>();

        if (targetIssueId <= 0 || wordSetByIssue == null || wordSetByIssue.isEmpty()) {
            return result;
        }

        HashSet<String> targetWordSet = wordSetByIssue.get(targetIssueId);

        if (targetWordSet == null || targetWordSet.isEmpty()) {
            return result;
        }

        for (Map.Entry<Long, HashSet<String>> entry : wordSetByIssue.entrySet()) {
            long issueId = entry.getKey();

            if (issueId == targetIssueId) {
                continue;
            }

            HashSet<String> otherWordSet = entry.getValue();

            double similarity = calculateJaccardSimilarity(targetWordSet, otherWordSet);
            result.put(issueId, similarity);
        }

        return result;
    }
}