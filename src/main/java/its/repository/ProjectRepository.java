package its.repository;
 
import java.util.List;
 
import its.model.Project;
 
public interface ProjectRepository {
    void save(Project project);
    Project findById(int projectId);
    List<Project> findAll();
    void update(Project project);
    void delete(int projectId);
    int generateProjectId();
}


