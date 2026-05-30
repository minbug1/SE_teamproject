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

    void saveAll(long projectId, List<Category> categories);

    List<Category> findByProjectId(long projectId);

    void clearByProjectId(long projectId);
}