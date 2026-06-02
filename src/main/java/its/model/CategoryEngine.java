package its.model;

import its.repository.CategoryRepository;

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
    final double MIN_INSTANT_CATEGORY_SIMILARITY = 0.125;

    private final TfIdf tfIdf = new TfIdf();
    private Set<String> vocabulary = new HashSet<>();

    private final CategoryRepository categoryRepository;

    // constructor
    public CategoryEngine(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
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
        int projectId = 0;
        for (Issue issue : Issues) {
            if (issue != null) {
                projectId = issue.getProjectId();
                break;
            }
        }

        // TF-IDF
        Map<Long, Map<String, Double>> tfIdfVectors = tfIdf.calculateTfIdfByIssue(Issues);
        this.vocabulary = tfIdf.getVocabulary();

        Map<String, Double> idfSnapshot = new HashMap<>(tfIdf.getIdf());

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
                        meanVector,
                        idfSnapshot
                );

                categories.add(newCategory);
                nextCategoryId++;
            }
        }

        return categories;
    }

    // instant classification
    public int categorizeSingleIssue(Issue newIssue, List<Category> savedCategories) {
        if (newIssue == null) {
            return 0;
        }

        if ((savedCategories == null || savedCategories.isEmpty()) && categoryRepository != null) {
            savedCategories = categoryRepository.findCategoriesByProjectId(newIssue.getProjectId());
        }

        if (savedCategories == null || savedCategories.isEmpty()) {
            return 0;
        }

        Set<String> storedVocabulary = new HashSet<>();
        Map<String, Double> storedIdf = new HashMap<>();

        for (Category category : savedCategories) {
            if (category == null) {
                continue;
            }

            if (category.getRepresentVector() != null) {
                storedVocabulary.addAll(category.getRepresentVector().keySet());
            }

            if (category.getIdf() != null) {
                storedVocabulary.addAll(category.getIdf().keySet());
                storedIdf.putAll(category.getIdf());
            }
        }

        if (storedVocabulary.isEmpty()) {
            return 0;
        }

        this.vocabulary = storedVocabulary;

        Map<String, Double> newIssueVector = tfIdf.calculateTfIdfByIssue(newIssue, storedVocabulary, storedIdf);

        int bestCategoryId = 0;
        double maxSimilarity = -1.0;

        for (Category category : savedCategories) {
            if (category == null || category.getRepresentVector() == null) {
                continue;
            }

            double similarity = tfIdf.cosineSimilarity(newIssueVector, category.getRepresentVector());

            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestCategoryId = category.getCategoryId();
            }
        }

        if (maxSimilarity < MIN_INSTANT_CATEGORY_SIMILARITY) {
            return 0;
        }

        return bestCategoryId; 
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
        Map<String, Double> idfSnapshot = new HashMap<>();
        if (categoryA.getIdf() != null) {
            idfSnapshot.putAll(categoryA.getIdf());
        }
        if (categoryB.getIdf() != null) {
            idfSnapshot.putAll(categoryB.getIdf());
        }

        return new Category(
                categoryA.getProjectId(),
                categoryIdA,
                categoryA.getThreshold(),
                mergedIssues,
                newMeanVector,
                idfSnapshot
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
        Map<String, Double> idfSnapshot = new HashMap<>();
        if (targetCategory.getIdf() != null) {
            idfSnapshot.putAll(targetCategory.getIdf());
        }

        Category updatedOriginalCategory = new Category(
                targetCategory.getProjectId(),
                targetCategoryId,
                targetCategory.getThreshold(),
                remainingIssues,
                remainingMean,
                idfSnapshot
        );

        Category newCategory = new Category(
                targetCategory.getProjectId(),
                newCategoryId,
                targetCategory.getThreshold(),
                separatingIssues,
                separatingMean,
                idfSnapshot
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