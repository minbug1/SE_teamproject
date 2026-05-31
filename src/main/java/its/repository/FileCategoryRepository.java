package its.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import its.model.Category;
import its.model.Issue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Repository implementation for CategoryRepository
 * 
 *
 * @author hanung
 */
public class FileCategoryRepository implements CategoryRepository {

    private final String directoryPath;
    private final IssueRepository issueRepository;
    private final Gson gson;

    public FileCategoryRepository(IssueRepository issueRepository) {
        this("data", issueRepository);
    }

    public FileCategoryRepository(String directoryPath, IssueRepository issueRepository) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory path must not be empty.");
        }
        if (issueRepository == null) {
            throw new IllegalArgumentException("IssueRepository must not be null.");
        }

        this.directoryPath = directoryPath;
        this.issueRepository = issueRepository;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        initializeDirectory();
    }

    private void initializeDirectory() {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private File getFileForProject(long projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Invalid project ID.");
        }
        return new File(directoryPath + File.separator + "project_" + projectId + "_categories.json");
    }

    @Override
    public void saveAll(long projectId, List<Category> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("Categories list must not be null.");
        }

        File file = getFileForProject(projectId);
        List<CategoryRecord> records = new ArrayList<>();

        for (Category category : categories) {
            if (category == null) {
                continue;
            }
            validateCategory(category);
            records.add(CategoryRecord.fromCategory(category));
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write category data file for project " + projectId, e);
        }
    }

    @Override
    public List<Category> findByProjectId(long projectId) {
        File file = getFileForProject(projectId);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        List<Issue> allProjectIssues = issueRepository.findAll(); 
        Map<Long, Issue> issueMap = new HashMap<>();
        for (Issue issue : allProjectIssues) {
            if (issue != null && issue.getProjectId() == projectId) {
                issueMap.put(issue.getIssueId(), issue);
            }
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<CategoryRecord>>() {}.getType();
            List<CategoryRecord> records = gson.fromJson(reader, listType);

            List<Category> categories = new ArrayList<>();
            if (records == null) {
                return categories;
            }

            for (CategoryRecord record : records) {
                categories.add(record.toCategory(projectId, issueMap));
            }

            return categories;

        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Failed to parse category data file. The JSON format is invalid.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read category data file for project " + projectId, e);
        }
    }

    @Override
    public void clearByProjectId(long projectId) {
        File file = getFileForProject(projectId);
        if (file.exists()) {
            file.delete();
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null.");
        }
        if (category.getCategoryId() < 0) {
            throw new IllegalArgumentException("Invalid category ID.");
        }
        if (category.getThreshold() < 0.0 || category.getThreshold() > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0.");
        }
    }

    // DTO
    private static class CategoryRecord {
        private int categoryId;
        private String categoryName;
        private double threshold;
        private List<Long> issueIds = new ArrayList<>();
        private Map<String, Double> representVector = new HashMap<>();

        private Category toCategory(long projectId, Map<Long, Issue> issueMap) {
            List<Issue> restoredIssues = new ArrayList<>();
            
            if (issueIds != null) {
                for (Long id : issueIds) {
                    Issue issue = issueMap.get(id);
                    if (issue != null) {
                        restoredIssues.add(issue);
                    }
                }
            }

            Category category = new Category(
                    projectId,
                    this.categoryId,
                    this.threshold,
                    restoredIssues,
                    this.representVector != null ? this.representVector : new HashMap<>()
            );
            
            category.setCategoryName(this.categoryName);
            return category;
        }

        private static CategoryRecord fromCategory(Category category) {
            CategoryRecord record = new CategoryRecord();
            record.categoryId = category.getCategoryId();
            record.categoryName = category.getCategoryName();
            record.threshold = category.getThreshold();

            if (category.getIssues() != null) { 
                for (Issue issue : category.getIssues()) {
                    if (issue != null) {
                        record.issueIds.add(issue.getIssueId());
                    }
                }
            }

            if (category.getRepresentVector() != null) {
                record.representVector = new HashMap<>(category.getRepresentVector());
            }

            return record;
        }
    }
}