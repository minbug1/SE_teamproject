package its.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class IssueSimilarity {
    
    // Jaccard similarity
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

    // cosine similarity
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
    
    // cosine similarity by issue
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

    // dot product
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

    // magnitude
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
}