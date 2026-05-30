package its.repository;

import its.model.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only in-memory implementation of ProjectRepository.
 * MemoryUserRepository와 동일한 방식으로 파일 I/O 없이 테스트에서 사용한다.
 */
public class MemoryProjectRepository implements ProjectRepository {

    private final List<Project> projects = new ArrayList<>();
    private int nextId = 1;

    @Override
    public void save(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null.");
        }
        for (Project existing : projects) {
            if (existing.getProjectId() == project.getProjectId()) {
                throw new IllegalArgumentException("Project ID already exists.");
            }
        }
        projects.add(project);
    }

    @Override
    public Project findById(int projectId) {
        if (projectId <= 0) return null;
        return projects.stream()
                .filter(p -> p.getProjectId() == projectId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Project> findAll() {
        return new ArrayList<>(projects);
    }

    @Override
    public void update(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null.");
        }
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getProjectId() == project.getProjectId()) {
                projects.set(i, project);
                return;
            }
        }
        throw new IllegalArgumentException("Project does not exist.");
    }

    @Override
    public void delete(int projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be positive.");
        }
        boolean removed = projects.removeIf(p -> p.getProjectId() == projectId);
        if (!removed) {
            throw new IllegalArgumentException("Project does not exist.");
        }
    }

    @Override
    public int generateProjectId() {
        return nextId++;
    }
}