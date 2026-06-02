package its.repository;

import its.model.Category;
import java.util.List;

/*
 * Repository interface for category
 * saveAll, findByProjectId, clearByProjectId
 *
 * @author hanung
 */
public interface CategoryRepository {

    void saveAll(int projectId, List<Category> categories);

    List<Category> findCategoriesByProjectId(int projectId);

    Category findCategoryById(int categoryId);

    void clearByProjectId(int projectId);
}