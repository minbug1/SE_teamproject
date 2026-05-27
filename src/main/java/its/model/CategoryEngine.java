package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * model for categorize
 * 
 * 
 * @author hanung
 */
public class CategoryEngine {

    private final TfIdf tfIdf = new TfIdf();
    private Set<String> vocabulary = new HashSet<>();

    // constructor
    public CategoryEngine() {}

    // full training
    public List<Category> createCategoriesByThreshold(List<Issue> Issues, double threshold) {
        List<Category> categories = new ArrayList<>();

        if (Issues == null || Issues.isEmpty()) {
            return categories;
        }

        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0.");
        }

        // extract project id
        long projectId = 0;
        for (Issue issue : Issues) {
            if (issue != null) {
                projectId = issue.getProjectId();
                break;
            }
        }

        // TF-IDF
        Map<Long, Map<String, Double>> tfIdfVectors = tfIdf.calculateTfIdfByIssue(Issues);
        this.vocabulary = tfIdf.getVocabulary();

        int nextCategoryId = 1;

        for (Issue issue : Issues) {
            if (issue == null) {
                continue;
            }

            Map<String, Double> issueVector = tfIdfVectors.get(issue.getIssueId());
            Category bestCategory = null;
            double bestSimilarity = -1.0;

            // search best category
            for (Category category : categories) {
                double similarity = tfIdf.cosineSimilarity(issueVector, category.getRepresentVector());

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestCategory = category;
                }
            }

            // classify or create
            if (bestCategory != null && bestSimilarity >= threshold) {
                issue.setCategoryId(bestCategory.getCategoryId());
                bestCategory.getIssues().add(issue);

                // update mean vector
                Map<String, Double> updatedMean = calculateMean(bestCategory.getIssues(), tfIdfVectors);
                bestCategory.getRepresentVector().clear();
                bestCategory.getRepresentVector().putAll(updatedMean);
            }
            else {
                issue.setCategoryId(nextCategoryId);

                List<Issue> categoryIssues = new ArrayList<>();
                categoryIssues.add(issue);

                Map<String, Double> meanVector = calculateMean(categoryIssues, tfIdfVectors);

                Category newCategory = new Category(
                        projectId,
                        nextCategoryId,
                        threshold,
                        categoryIssues,
                        meanVector
                );

                categories.add(newCategory);
                nextCategoryId++;
            }
        }

        return categories;
    }

    // instant classification
    public int categorizeSingleIssue(Issue newIssue, List<Category> savedCategories, List<Issue> Issues) {
        if (newIssue == null || savedCategories == null || savedCategories.isEmpty()) {
            return 0; 
        }

        // no idf -> build
        if (tfIdf.getIdf() == null || tfIdf.getIdf().isEmpty()) {
            if (Issues != null && !Issues.isEmpty()) {
                double threshold = savedCategories.get(0).getThreshold();
                
                buildCategories(newIssue.getProjectId(), Issues, threshold);
            }
        }

        // use category keyset
        if (this.vocabulary.isEmpty()) {
            for (Category category : savedCategories) {
                if (category != null && category.getRepresentVector() != null) {
                    this.vocabulary.addAll(category.getRepresentVector().keySet());
                }
            }
        }
        
        // new issue vector
        Map<String, Double> newIssueVector = calculateSingleIssueTfIdf(newIssue, this.vocabulary);

        int bestCategoryId = 0;
        double maxSimilarity = -1.0;
        Category bestCategory = null;
        double threshold = savedCategories.get(0).getThreshold();

        for (Category category : savedCategories) {
            double similarity = tfIdf.cosineSimilarity(newIssueVector, category.getRepresentVector());

            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestCategoryId = category.getCategoryId();
                bestCategory = category; 
            }
        }

        if (bestCategory != null && maxSimilarity >= threshold) {
            return bestCategoryId;
        }

        return 0; 
    }

    // rebuild category
    public List<Category> buildCategories(long projectId, List<Issue> Issues, double threshold) {
        List<Category> builtCategories = new ArrayList<>();
        if (Issues == null || Issues.isEmpty()) {
            return builtCategories;
        }

        Map<Long, Map<String, Double>> tfIdfVectors = tfIdf.calculateTfIdfByIssue(Issues);
        this.vocabulary = tfIdf.getVocabulary();

        // grouping
        Map<Integer, List<Issue>> issuesByCategory = new HashMap<>();
        for (Issue issue : Issues) {
            if (issue == null || issue.getCategoryId() <= 0) {
                continue;
            }
            issuesByCategory.computeIfAbsent(issue.getCategoryId(), k -> new ArrayList<>()).add(issue);
        }

        // build category
        for (Map.Entry<Integer, List<Issue>> entry : issuesByCategory.entrySet()) {
            int categoryId = entry.getKey();
            List<Issue> categoryIssues = entry.getValue();

            Map<String, Double> meanVector = calculateMean(categoryIssues, tfIdfVectors);

            Category newCategorySnapshot = new Category(
                    projectId,
                    categoryId,
                    threshold,
                    categoryIssues,
                    meanVector
            );

            builtCategories.add(newCategorySnapshot);
        }

        return builtCategories;
    }

    // calculate mean
    private Map<String, Double> calculateMean(List<Issue> categoryIssues, Map<Long, Map<String, Double>> tfIdfVectors) {
        Map<String, Double> mean = new HashMap<>();

        if (categoryIssues == null || categoryIssues.isEmpty()) {
            for (String word : this.vocabulary) {
                mean.put(word, 0.0);
            }
            return mean;
        }
        
        for (String word : this.vocabulary) {
            double sum = 0.0;
            for (Issue issue : categoryIssues) {
                Map<String, Double> issueVector = tfIdfVectors.get(issue.getIssueId());
                if (issueVector != null) {
                    sum += issueVector.getOrDefault(word, 0.0);
                }
            }
            mean.put(word, sum / categoryIssues.size());
        }
        return mean;
    }

    // calculate single issue TF-IDF
    public Map<String, Double> calculateSingleIssueTfIdf(Issue issue, Set<String> vocabulary) {
        Map<String, Double> singleVector = new HashMap<>();
        if (issue == null) return singleVector;

        Map<String, Integer> issueWordCounts = tfIdf.countWordsByIssue(issue);
        tfIdf.cutWords(issueWordCounts, 3);

        int totalWeightCount = 0;
        for (Integer count : issueWordCounts.values()) {
            if (count != null) {
                totalWeightCount += count.intValue();
            }
        }
        
        Map<String, Double> storedIdfMap = tfIdf.getIdf();

        for (String word : vocabulary) {
            if (totalWeightCount > 0 && issueWordCounts.containsKey(word)) {
                double tf = (double) issueWordCounts.get(word) / totalWeightCount;
                double idfValue = storedIdfMap.getOrDefault(word, 1.0);
                singleVector.put(word, tf * idfValue); 
            } else {
                singleVector.put(word, 0.0);
            }
        }
        return singleVector;
    }

    // merge
    public Category mergeCategories(int categoryIdA, int categoryIdB, List<Category> savedCategories, Map<Long, Map<String, Double>> tfIdfVectors) {
        Category categoryA = null;
        Category categoryB = null;

        for (Category category : savedCategories) {
            if (category.getCategoryId() == categoryIdA) {
                categoryA = category;
            }
            if (category.getCategoryId() == categoryIdB) {
                categoryB = category;
            }
        }

        if (categoryA == null || categoryB == null) {
            return categoryA;
        }

        // integrate
        List<Issue> mergedIssues = new ArrayList<>();
        mergedIssues.addAll(categoryA.getIssues());
        mergedIssues.addAll(categoryB.getIssues());

        // set category id
        for (Issue issue : mergedIssues) {
            if (issue != null) {
                issue.setCategoryId(categoryIdA);
            }
        }

        // new mean vector
        Map<String, Double> newMeanVector = calculateMean(mergedIssues, tfIdfVectors);

        // build category
        return new Category(
                categoryA.getProjectId(),
                categoryIdA,
                categoryA.getThreshold(),
                mergedIssues,
                newMeanVector
        );
    }

    // partition
    public List<Category> partitionCategoryA(int targetCategoryId, 
                                             List<Issue> remainingIssues, List<Issue> separatingIssues, 
                                             List<Category> savedCategories, Map<Long, Map<String, Double>> tfIdfVectors) {
        
        List<Category> partitionedResult = new ArrayList<>();
        
        if (savedCategories == null || savedCategories.isEmpty()) {
            return partitionedResult;
        }

        // search category
        Category targetCategory = null;
        int maxCategoryId = 0;

        for (Category category : savedCategories) {
            if (category.getCategoryId() == targetCategoryId) {
                targetCategory = category;
            }
            if (category.getCategoryId() > maxCategoryId) {
                maxCategoryId = category.getCategoryId();
            }
        }

        // target validation
        if (targetCategory == null) {
            return partitionedResult;
        }
        // new category id
        int newCategoryId = maxCategoryId + 1;

        // separate partition
        if (separatingIssues != null) {
            for (Issue issue : separatingIssues) {
                if (issue != null) {
                    issue.setCategoryId(newCategoryId);
                }
            }
        }

        // remain validation        
        if (remainingIssues != null) {
            for (Issue issue : remainingIssues) {
                if (issue != null) {
                    issue.setCategoryId(targetCategoryId);
                }
            }
        }

        // new mean vector
        Map<String, Double> remainingMean = calculateMean(remainingIssues != null ? remainingIssues : new ArrayList<>(), tfIdfVectors);
        Map<String, Double> separatingMean = calculateMean(separatingIssues != null ? separatingIssues : new ArrayList<>(), tfIdfVectors);

        // build category
        Category updatedOriginalCategory = new Category(
                targetCategory.getProjectId(),
                targetCategoryId,
                targetCategory.getThreshold(),
                remainingIssues,
                remainingMean
        );

        Category newCategory = new Category(
                targetCategory.getProjectId(),
                newCategoryId,
                targetCategory.getThreshold(),
                separatingIssues,
                separatingMean
        );

        partitionedResult.add(updatedOriginalCategory);
        partitionedResult.add(newCategory);

        return partitionedResult;
    }

    // reset
    public void resetCategory(List<Issue> Issues) {
        if (Issues == null || Issues.isEmpty()) {
            this.vocabulary.clear();
            return;
        }

        for (Issue issue : Issues) {
            if (issue != null) {
                issue.setCategoryId(0); 
            }
        }
        
        this.vocabulary.clear();
    }

    // get
    public Set<String> getVocabulary() {
        return this.vocabulary;
    }

    public TfIdf getTfIdf() {
        return this.tfIdf;
    }
}