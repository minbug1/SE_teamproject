package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine for Category Classification and Vector Operations
 * 
 * * @author hanung
 */
public class CategoryEngine {

    // 한웅님이 작성하신 원본 상태 그대로의 TfIdf 계산기 인스턴스 유지
    private final TfIdf tfIdf = new TfIdf();
    
    // 엔진 레벨에서 즉석 복원 및 코사인 유사도 순회에 사용할 단어장 캐시
    private final Set<String> globalVocabulary = new HashSet<>();

    // ==========================================
    //      1. 단일 미분류 이슈 실시간 즉시 분류
    // ==========================================
    public int categorizeSingleIssue(Issue newIssue, List<Category> savedCategories) {
        if (newIssue == null || savedCategories == null || savedCategories.isEmpty()) {
            return 0; 
        }

        // 프로그램 실행 직후라 엔진의 단어장이 비어있다면, 불러온 카테고리 정보의 키셋으로 즉석 복원
        if (this.globalVocabulary.isEmpty()) {
            this.globalVocabulary.addAll(savedCategories.get(0).getRepresentVector().keySet());
        }

        // 복원된 단어장을 기반으로 단일 이슈 TF 벡터 생성
        Map<String, Double> newIssueVector = calculateSingleIssueTfIdf(newIssue, this.globalVocabulary);

        int bestCategoryId = 0;
        double maxSimilarity = -1.0;
        Category bestCategory = null;

        for (Category category : savedCategories) {
            double similarity = calculateCosineSimilarity(newIssueVector, category.getRepresentVector());

            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestCategoryId = category.getCategoryId();
                bestCategory = category; 
            }
        }

        // 해당 카테고리 스냅샷 정보에서 고유 임계치(Threshold)를 직접 읽어와 검증
        if (bestCategory != null && maxSimilarity >= bestCategory.getCosineThreshold()) {
            return bestCategoryId;
        }

        return 0; 
    }

    // ==========================================
    //      2. 전체 이슈 기반 카테고리 통째로 재학습
    // ==========================================
    public List<Category> trainCategories(long projectId, List<Issue> allIssues, double targetThreshold) {
        List<Category> trainedCategories = new ArrayList<>();
        if (allIssues == null || allIssues.isEmpty()) {
            return trainedCategories;
        }

        // ★ [에러 해결 핵심] 한웅님의 원본 함수 포맷 그대로 인자를 1개만 전달!
        // 이 함수 호출이 끝나면 tfIdf 내부의 vocabulary 변수가 완벽하게 최신 상태로 구워집니다.
        Map<Long, Map<String, Double>> tfIdfVectors = tfIdf.calculateTfIdfByIssue(allIssues);

        // ★ 엔진 상단의 globalVocabulary 세트도 굳이 복잡하게 새로 빌드할 필요 없이, 
        // 방금 계산이 끝난 카테고리 벡터 중 하나의 키셋을 가져와 깔끔하게 동기화합니다.
        this.globalVocabulary.clear();
        if (!tfIdfVectors.isEmpty()) {
            // 아무 이슈 벡터나 하나 잡아서 키셋을 복사하면 전역 단어장 완벽 셋업
            Map<String, Double> randomVector = tfIdfVectors.values().iterator().next();
            this.globalVocabulary.addAll(randomVector.keySet());
        }

        // 카테고리별 그루핑
        Map<Integer, List<Issue>> issuesByCategory = new HashMap<>();
        for (Issue issue : allIssues) {
            if (issue == null || issue.getCategoryId() <= 0) continue;
            issuesByCategory.computeIfAbsent(issue.getCategoryId(), k -> new ArrayList<>()).add(issue);
        }

        // 카테고리 대표 중심(Centroid) 벡터 연산 및 새 불변 스냅샷 빌드
        for (Map.Entry<Integer, List<Issue>> entry : issuesByCategory.entrySet()) {
            int categoryId = entry.getKey();
            List<Issue> categoryIssues = entry.getValue();

            Map<String, Double> centroidVector = calculateCentroid(categoryIssues, tfIdfVectors);

            Category newCategorySnapshot = new Category(
                    projectId,
                    categoryId,
                    targetThreshold,
                    categoryIssues,
                    centroidVector
            );

            trainedCategories.add(newCategorySnapshot);
        }

        return trainedCategories;
    }

    // ==========================================
    //              내부 연산 헬퍼 메서드
    // ==========================================

    private Map<String, Double> calculateCentroid(List<Issue> categoryIssues, Map<Long, Map<String, Double>> tfIdfVectors) {
        Map<String, Double> centroid = new HashMap<>();
        
        for (String word : this.globalVocabulary) {
            double sum = 0.0;
            for (Issue issue : categoryIssues) {
                Map<String, Double> issueVector = tfIdfVectors.get(issue.getIssueId());
                if (issueVector != null) {
                    sum += issueVector.getOrDefault(word, 0.0);
                }
            }
            centroid.put(word, sum / categoryIssues.size());
        }
        return centroid;
    }

    private Map<String, Double> calculateSingleIssueTfIdf(Issue issue, Set<String> vocabulary) {
        Map<String, Double> singleVector = new HashMap<>();
        if (issue == null) return singleVector;

        // 단일 이슈용 가중치 계산 (TfIdf 내부의 문자열 전처리 및 가중치 사상을 모방)
        Map<String, Integer> issueWordCounts = countWordsForSingle(issue);
        int totalWeightCount = issueWordCounts.values().stream().mapToInt(Integer::intValue).sum();

        for (String word : vocabulary) {
            if (totalWeightCount > 0 && issueWordCounts.containsKey(word)) {
                singleVector.put(word, (double) issueWordCounts.get(word) / totalWeightCount);
            } else {
                singleVector.put(word, 0.0);
            }
        }
        return singleVector;
    }

    private double calculateCosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : this.globalVocabulary) {
            double valA = vectorA.getOrDefault(word, 0.0);
            double valB = vectorB.getOrDefault(word, 0.0);

            dotProduct += valA * valB;
            normA += valA * valA;
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<String, Integer> countWordsForSingle(Issue issue) {
        Map<String, Integer> wordCount = new HashMap<>();
        if (issue == null) return wordCount;

        // 단일 이슈 전처리용 간단 로직 (한웅님 소스코드 기반)
        addTextForSingle(wordCount, issue.getTitle(), 3);        // titleWeight
        addTextForSingle(wordCount, issue.getDescription(), 2);  // descriptionWeight

        if (issue.getComments() != null) {
            for (its.model.Comment comment : issue.getComments()) {
                if (comment != null) {
                    addTextForSingle(wordCount, comment.getContent(), 1); // commentWeight
                }
            }
        }
        // MIN_WORD_COUNT 컷오프 처리
        wordCount.entrySet().removeIf(entry -> entry.getValue() < 3);
        return wordCount;
    }

    private void addTextForSingle(Map<String, Integer> wordCount, String text, int weight) {
        if (text == null || text.trim().isEmpty()) return;
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            if (!token.trim().isEmpty()) {
                wordCount.put(token, wordCount.getOrDefault(token, 0) + weight);
            }
        }
    }
}