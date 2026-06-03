package its.repository;

import its.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Test-only in-memory implementation
 * Override FileaCategoryRepository
 *
 * @author hanung
 */
public class MemoryCategoryRepository implements CategoryRepository {

    // memory
    private final Map<Integer, List<Category>> categoriesByProjectId = new HashMap<>();

    @Override
    public void saveAll(int projectId, List<Category> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("Categories list must not be null.");
        }

        List<Category> copiedCategories = new ArrayList<>();

        for (Category category : categories) {
            if (category == null) {
                continue;
            }

            validateCategory(category);
            copiedCategories.add(category);
        }

        categoriesByProjectId.put(projectId, copiedCategories);
    }

    @Override
    public List<Category> findCategoriesByProjectId(int projectId) {
        if (projectId <= 0) {
            return new ArrayList<>();
        }

        List<Category> categories = categoriesByProjectId.get(projectId);

        if (categories == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(categories);
    }

    @Override
    public Category findCategoryById(int categoryId) {
        if (categoryId < 0) {
            return null;
        }

        for (List<Category> categories : categoriesByProjectId.values()) {
            for (Category category : categories) {
                if (category != null && category.getCategoryId() == categoryId) {
                    return category;
                }
            }
        }

        return null;
    }

    @Override
    public void clearByProjectId(int projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Invalid project ID.");
        }

        categoriesByProjectId.remove(projectId);
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
}