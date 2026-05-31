package its.controller;

import its.model.Category;
import its.model.CategoryEngine;
import its.model.Issue;
import its.model.Project;
import its.model.User;
import its.model.UserRole;
import its.repository.CategoryRepository;
import its.repository.IssueRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * controller for category management
 *
 * @author hanung
 */
public class CategoryController {

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryEngine categoryEngine;

    // constructor
    public CategoryController(IssueRepository issueRepository, CategoryRepository categoryRepository) {
        if (issueRepository == null || categoryRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null.");
        }
        this.issueRepository = issueRepository;
        this.categoryRepository = categoryRepository;
        this.categoryEngine = new CategoryEngine();
    }

    // create categories
    public List<Category> createCategories(Project project, double threshold, User pl) {
        validatePL(project, pl);
        validateThreshold(threshold);

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        if (projectIssues == null || projectIssues.isEmpty()) {
            return new ArrayList<>();
        }

        List<Category> categories = categoryEngine.createCategoriesByThreshold(projectIssues, threshold);

        saveAndSync(project.getProjectId(), categories, projectIssues);

        return categories;
    }

    public List<Category> previewCategories(Project project, double threshold, User pl) {
        validatePL(project, pl);
        validateThreshold(threshold);

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        if (projectIssues == null || projectIssues.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> originalCategoryIds = new ArrayList<>();
        for (Issue issue : projectIssues) {
            originalCategoryIds.add(issue != null ? issue.getCategoryId() : 0);
        }

        List<Category> categories = categoryEngine.createCategoriesByThreshold(projectIssues, threshold);

        for (int i = 0; i < projectIssues.size(); i++) {
            Issue issue = projectIssues.get(i);
            if (issue != null) {
                issue.setCategoryId(originalCategoryIds.get(i));
            }
        }

        return categories;
    }

    // find categories
    public List<Category> findCategories(Project project, User pl) {
        validatePL(project, pl);

        return categoryRepository.findByProjectId(project.getProjectId());
    }

    public List<Category> saveCategories(Project project, List<Category> categories, User pl) {
        validatePL(project, pl);
        if (categories == null) {
            throw new IllegalArgumentException("Categories must not be null.");
        }

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        Map<Long, Issue> projectIssueMap = new HashMap<>();
        for (Issue issue : projectIssues) {
            if (issue != null) {
                issue.setCategoryId(0);
                projectIssueMap.put(issue.getIssueId(), issue);
            }
        }

        Set<Long> assignedIssueIds = new HashSet<>();
        for (Category category : categories) {
            if (category == null) {
                continue;
            }

            validateCategoryId(category.getCategoryId());
            if (category.getIssues() == null) {
                continue;
            }

            for (Issue issue : category.getIssues()) {
                if (issue == null) {
                    continue;
                }

                Issue projectIssue = projectIssueMap.get(issue.getIssueId());
                if (projectIssue == null) {
                    throw new IllegalArgumentException("Issue does not belong to this project.");
                }
                if (!assignedIssueIds.add(issue.getIssueId())) {
                    throw new IllegalArgumentException("Issue belongs to multiple categories.");
                }

                issue.setCategoryId(category.getCategoryId());
                projectIssue.setCategoryId(category.getCategoryId());
            }
        }

        saveAndSync(project.getProjectId(), categories, projectIssues);
        if (categories.isEmpty()) {
            categoryRepository.clearByProjectId(project.getProjectId());
        }

        return categories;
    }

    public Category createCategory(Project project, String categoryName, User pl) {
        validatePL(project, pl);

        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid category name.");
        }

        List<Category> savedCategories = categoryRepository.findByProjectId(project.getProjectId());
        String trimmedName = categoryName.trim();
        for (Category category : savedCategories) {
            if (category != null && trimmedName.equalsIgnoreCase(category.getCategoryName())) {
                throw new IllegalArgumentException("Category name already exists.");
            }
        }

        int nextCategoryId = 1;
        for (Category category : savedCategories) {
            if (category != null && category.getCategoryId() >= nextCategoryId) {
                nextCategoryId = category.getCategoryId() + 1;
            }
        }

        Category category = new Category(
                project.getProjectId(),
                nextCategoryId,
                extractThreshold(savedCategories),
                new ArrayList<>(),
                new java.util.HashMap<>()
        );
        category.setCategoryName(trimmedName);

        savedCategories.add(category);
        categoryRepository.saveAll(project.getProjectId(), savedCategories);

        return category;
    }

    // reset categories
    public void resetCategories(Project project, User pl) {
        validatePL(project, pl);

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        categoryEngine.resetCategory(projectIssues);

        saveAndSync(project.getProjectId(), new ArrayList<>(), projectIssues);
        categoryRepository.clearByProjectId(project.getProjectId());
    }

    // merge categories
    public List<Category> mergeCategories(Project project, int categoryIdA, int categoryIdB, User pl) {
        validatePL(project, pl);
        validateCategoryId(categoryIdA);
        validateCategoryId(categoryIdB);
        if (categoryIdA == categoryIdB) {
            throw new IllegalArgumentException("Cannot merge same category.");
        }

        List<Category> savedCategories = categoryRepository.findByProjectId(project.getProjectId());
        if (savedCategories == null || savedCategories.isEmpty()) {
            return new ArrayList<>();
        }

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());

        // calculate vector and merge
        Map<Long, Map<String, Double>> tfIdfVectors = categoryEngine.getTfIdf().calculateTfIdfByIssue(projectIssues);
        Category mergedCategory = categoryEngine.mergeCategories(categoryIdA, categoryIdB, savedCategories, tfIdfVectors);

        if (mergedCategory == null) {
            return savedCategories;
        }

        // update list
        List<Category> result = new ArrayList<>();
        for (Category category : savedCategories) {
            if (category == null) {
                continue;
            }

            if (category.getCategoryId() == categoryIdA) {
                result.add(mergedCategory);
            }
            else if (category.getCategoryId() != categoryIdB) {
                result.add(category);
            }
        }

        saveAndSync(project.getProjectId(), result, projectIssues);

        return result;
    }

    public List<Category> previewMergeCategories(Project project, List<Category> categories,
                                                 int categoryIdA, int categoryIdB, User pl) {
        validatePL(project, pl);
        validateCategoryId(categoryIdA);
        validateCategoryId(categoryIdB);
        if (categoryIdA == categoryIdB) {
            throw new IllegalArgumentException("Cannot merge same category.");
        }
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        Category targetCategory = findCategoryById(categories, categoryIdA);
        String targetName = targetCategory != null ? targetCategory.getCategoryName() : null;
        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        Map<Long, Map<String, Double>> tfIdfVectors = categoryEngine.getTfIdf().calculateTfIdfByIssue(projectIssues);
        Category mergedCategory = categoryEngine.mergeCategories(categoryIdA, categoryIdB, categories, tfIdfVectors);

        if (mergedCategory == null) {
            return categories;
        }
        mergedCategory.setCategoryName(targetName);

        List<Category> result = new ArrayList<>();
        for (Category category : categories) {
            if (category == null) {
                continue;
            }

            if (category.getCategoryId() == categoryIdA) {
                result.add(mergedCategory);
            }
            else if (category.getCategoryId() != categoryIdB) {
                result.add(category);
            }
        }

        return result;
    }

    // partition category
    public List<Category> partitionCategory(Project project, int targetCategoryId, List<Long> separatingIssueIds, User pl) {
        validatePL(project, pl);
        validateCategoryId(targetCategoryId);
        if (separatingIssueIds == null || separatingIssueIds.isEmpty()) {
            throw new IllegalArgumentException("Issue IDs empty.");
        }

        List<Category> savedCategories = categoryRepository.findByProjectId(project.getProjectId());
        Category targetCategory = findCategoryById(savedCategories, targetCategoryId);
        if (targetCategory == null) {
            throw new IllegalArgumentException("Target category does not exist.");
        }

        // extract separate partition
        List<Issue> remainingIssues = new ArrayList<>();
        List<Issue> separatingIssues = new ArrayList<>();
        for (Issue issue : targetCategory.getIssues()) {
            if (issue == null) {
                continue;
            }

            if (separatingIssueIds.contains(issue.getIssueId())) {
                separatingIssues.add(issue);
            }
            else {
                remainingIssues.add(issue);
            }
        }

        if (separatingIssues.isEmpty()) {
            throw new IllegalArgumentException("No issue selected.");
        }

        if (remainingIssues.isEmpty()) {
            throw new IllegalArgumentException("Original category cannot be empty.");
        }

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        Map<Long, Map<String, Double>> tfIdfVectors = categoryEngine.getTfIdf().calculateTfIdfByIssue(projectIssues);

        // split mapping
        List<Category> partitioned = categoryEngine.partitionCategoryA(targetCategoryId, remainingIssues, separatingIssues, savedCategories, tfIdfVectors);

        List<Category> result = new ArrayList<>();
        for (Category category : savedCategories) {
            if (category == null) {
                continue;
            }

            if (category.getCategoryId() == targetCategoryId) {
                result.addAll(partitioned);
            }
            else {
                result.add(category);
            }
        }

        saveAndSync(project.getProjectId(), result, projectIssues);

        return result;
    }

    public List<Category> previewPartitionCategory(Project project, List<Category> categories,
                                                   int targetCategoryId, List<Long> separatingIssueIds, User pl) {
        validatePL(project, pl);
        validateCategoryId(targetCategoryId);
        if (separatingIssueIds == null || separatingIssueIds.isEmpty()) {
            throw new IllegalArgumentException("Issue IDs empty.");
        }
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        Category targetCategory = findCategoryById(categories, targetCategoryId);
        if (targetCategory == null) {
            throw new IllegalArgumentException("Target category does not exist.");
        }

        List<Issue> remainingIssues = new ArrayList<>();
        List<Issue> separatingIssues = new ArrayList<>();
        for (Issue issue : targetCategory.getIssues()) {
            if (issue == null) {
                continue;
            }

            if (separatingIssueIds.contains(issue.getIssueId())) {
                separatingIssues.add(issue);
            }
            else {
                remainingIssues.add(issue);
            }
        }

        if (separatingIssues.isEmpty()) {
            throw new IllegalArgumentException("No issue selected.");
        }

        if (remainingIssues.isEmpty()) {
            throw new IllegalArgumentException("Original category cannot be empty.");
        }

        List<Issue> projectIssues = issueRepository.findByProjectId(project.getProjectId());
        Map<Long, Map<String, Double>> tfIdfVectors = categoryEngine.getTfIdf().calculateTfIdfByIssue(projectIssues);
        List<Category> partitioned = categoryEngine.partitionCategoryA(
                targetCategoryId, remainingIssues, separatingIssues, categories, tfIdfVectors);
        if (!partitioned.isEmpty()) {
            partitioned.get(0).setCategoryName(targetCategory.getCategoryName());
        }

        List<Category> result = new ArrayList<>();
        for (Category category : categories) {
            if (category == null) {
                continue;
            }

            if (category.getCategoryId() == targetCategoryId) {
                result.addAll(partitioned);
            }
            else {
                result.add(category);
            }
        }

        return result;
    }
    
    // update category name
    public Category updateCategoryName(Project project, int categoryId, String newName, User pl) {
        validatePL(project, pl);
        validateCategoryId(categoryId);

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid category name.");
        }

        List<Category> savedCategories = categoryRepository.findByProjectId(project.getProjectId());
        Category targetCategory = findCategoryById(savedCategories, categoryId);
        
        // target validation
        if (targetCategory == null) {
            throw new IllegalArgumentException("Target category does not exist.");
        }

        // duplicate check
        String trimmedName = newName.trim();
        for (Category category : savedCategories) {
            if (category == null) continue;
            if (category.getCategoryId() != categoryId && trimmedName.equalsIgnoreCase(category.getCategoryName())) {
                throw new IllegalArgumentException("Category name already exists.");
            }
        }

        // update name and save
        targetCategory.setCategoryName(trimmedName);
        categoryRepository.saveAll(project.getProjectId(), savedCategories);

        return targetCategory;
    }

    // helper methods
    private void saveAndSync(long projectId, List<Category> categories, List<Issue> issues) {
        if (categories != null && !categories.isEmpty()) {
            categoryRepository.saveAll(projectId, categories);
        }

        if (issues != null) {
            for (Issue issue : issues) {
                if (issue != null) {
                    issueRepository.update(issue);
                }
            }
        }
    }

    private Category findCategoryById(List<Category> categories, int id) {
        if (categories == null) {
            return null;
        }

        for (Category c : categories) {
            if (c != null && c.getCategoryId() == id) {
                return c;
            }
        }

        return null;
    }

    private double extractThreshold(List<Category> categories) {
        if (categories == null || categories.isEmpty()) return 0.25;
        for (Category c : categories) {
            if (c != null) {
                return c.getThreshold();
            }
        }
        
        return 0.25;
    }

    private void validatePL(Project project, User pl) {
        if (project == null || pl == null) {
            throw new IllegalArgumentException("Arguments must not be null.");
        }

        if (pl.getRole() != UserRole.PL) {
            throw new SecurityException("Only PL can manage categories.");
        }

        if (!project.getMembers().contains(pl)) {
            throw new SecurityException("PL must be a project member.");
        }
    }

    private void validateThreshold(double t) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Threshold invalid.");
        }
    }

    private void validateCategoryId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Category ID must be positive.");
        }
    }
}
