package its.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import its.model.Issue;
import its.model.Project;
import its.model.User;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FileProjectRepository implements ProjectRepository {

    private static final String DEFAULT_FILE_PATH = "data/projects.json";

    private final File file;
    private final Gson gson;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public FileProjectRepository() {
        this(DEFAULT_FILE_PATH, null, null);
    }

    public FileProjectRepository(UserRepository userRepository, IssueRepository issueRepository) {
        this(DEFAULT_FILE_PATH, userRepository, issueRepository);
    }

    public FileProjectRepository(String filePath, UserRepository userRepository, IssueRepository issueRepository) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty.");
        }

        this.file = new File(filePath);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;

        initializeFile();
    }

    private void initializeFile() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            if (file.length() == 0) {
                writeAll(new ArrayList<>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize project data file.", e);
        }
    }


    @Override
    public void save(Project project) {
        validateProject(project);

        List<Project> projects = findAll();

        for (Project existing : projects) {
            if (existing.getProjectId() == project.getProjectId()) {
                throw new IllegalArgumentException("Project ID already exists.");
            }
        }

        projects.add(project);
        writeAll(projects);
    }

    @Override
    public Project findById(int projectId) {
        if (projectId <= 0) return null;

        for (Project project : findAll()) {
            if (project.getProjectId() == projectId) return project;
        }
        return null;
    }

    @Override
    public List<Project> findAll() {
        List<ProjectRecord> records = readAllRecords();
        List<Project> projects = new ArrayList<>();

        for (ProjectRecord record : records) {
            projects.add(record.toProject(userRepository, issueRepository));
        }
        return projects;
    }

    @Override
    public void update(Project project) {
        validateProject(project);

        List<Project> projects = findAll();
        boolean updated = false;

        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getProjectId() == project.getProjectId()) {
                projects.set(i, project);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("Project does not exist.");
        }

        writeAll(projects);
    }

    @Override
    public void delete(int projectId) {
        if (projectId <= 0) throw new IllegalArgumentException("Project ID must be positive.");

        List<Project> projects = findAll();
        boolean deleted = projects.removeIf(p -> p.getProjectId() == projectId);

        if (!deleted) throw new IllegalArgumentException("Project does not exist.");

        writeAll(projects);
    }

    @Override
    public int generateProjectId() {
        int maxId = 0;
        for (Project project : findAll()) {
            if (project.getProjectId() > maxId) maxId = project.getProjectId();
        }
        return maxId + 1;
    }


    private List<ProjectRecord> readAllRecords() {
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<ProjectRecord>>() {}.getType();
            List<ProjectRecord> records = gson.fromJson(reader, listType);
            return records != null ? records : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Failed to parse project data file. The JSON format is invalid.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project data file.", e);
        }
    }

    private void writeAll(List<Project> projects) {
        List<ProjectRecord> records = new ArrayList<>();
        for (Project project : projects) {
            records.add(ProjectRecord.fromProject(project));
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write project data file.", e);
        }
    }

    private void validateProject(Project project) {
        if (project == null) throw new IllegalArgumentException("Project must not be null.");
        if (project.getProjectId() <= 0) throw new IllegalArgumentException("Project ID must be positive.");
    }


    private static class ProjectRecord {
        private int projectId;
        private String name;
        private String description;
        private List<Long> memberIds;   // User ID 목록만 저장
        private List<Long> issueIds; // Issue ID 목록만 저장

        private Project toProject(UserRepository userRepo, IssueRepository issueRepo) {
            Project project = new Project(projectId, name, description);

            // memberIds → User 객체 복원
            if (memberIds != null && userRepo != null) {
                for (Long memberId : memberIds) {
                    User user = userRepo.findByUserId(memberId);
                    if (user != null) project.addMember(user);
                }
            }

            // issueIds -> Issue 객체 복원
            if (issueIds != null) {
                for (long issueId : issueIds) {
                    if (issueId == 0) {
                        continue;
                    }

                    project.addIssueId(issueId);
                    if (issueRepo != null) {
                        Issue issue = issueRepo.findByProjectIdAndIssueId(projectId, issueId);
                        if (issue == null) {
                            issue = issueRepo.findById(issueId);
                            if (issue != null && issue.getProjectId() == 0) {
                                issue.setProjectId(projectId);
                            }
                        }
                        if (issue != null) project.addIssue(issue);
                    }
                }
            }

            return project;
        }

        private static ProjectRecord fromProject(Project project) {
            ProjectRecord r = new ProjectRecord();
            r.projectId   = project.getProjectId();
            r.name        = project.getName();
            r.description = project.getDescription();

            // User 객체 → ID만 추출
            r.memberIds = new ArrayList<>();
            for (User member : project.getMembers()) {
                r.memberIds.add(member.getUserId());
            }

            // Issue 객체 → ID만 추출
            r.issueIds = new ArrayList<>();
            for (Issue issue : project.getIssues()) {
                if (issue.getIssueId() != 0) {
                    r.issueIds.add(issue.getIssueId());
                }
            }
            for (Long issueId : project.getIssueIds()) {
                if (issueId != null && !r.issueIds.contains(issueId)) {
                    r.issueIds.add(issueId);
                }
            }

            return r;
        }
    }
}
