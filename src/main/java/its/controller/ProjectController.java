package its.controller;

import java.util.List;

import its.model.Issue;
import its.model.Project;
import its.model.User;
import its.repository.FileProjectRepository;
import its.repository.ProjectRepository;

public class ProjectController {

    private ProjectRepository projectRepository;

    // 기본 생성자에서 FileProjectRepository를 사용하도록 설정
    public ProjectController() {
       this(new FileProjectRepository());
    }

    // 의존성 주입을 위한 생성자
    public ProjectController(ProjectRepository projectRepository) {
        if (projectRepository == null) {
            throw new IllegalArgumentException("Project repository must not be null.");
        }
        this.projectRepository = projectRepository;
    }

    //admin이 프로젝트 생성
    public Project createProject(String name, String description, User adminUser) {
        
        // Admin 권한 검증
        validateAdmin(adminUser);

        // 유효성 검증
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("프로젝트 이름은 필수입니다.");
        }

        // 프로젝트 객체 생성
        int newProjectId = projectRepository.generateProjectId();
        Project newProject = new Project(newProjectId, name, description);

        // 프로젝트 저장 (파일 저장)
        projectRepository.save(newProject); 

        return newProject;
    }

    public void updateProject(Project project, User adminUser) {
        if (adminUser == null || !adminUser.isAdmin()) {
            throw new SecurityException("Project update permission is required.");
        }
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null.");
        }
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required.");
        }

        projectRepository.update(project);
    }

    //멤버 추가
    public void addMemberToProject(Project project, User newMember, User adminUser) {
        
        validateAdmin(adminUser);
        validateProject(project);

        if (newMember == null) {
            throw new IllegalArgumentException("추가할 유저 정보가 없습니다.");
        }

        // 프로젝트에 멤버 추가
        project.addMember(newMember);

        //프로젝트 업데이트 (파일 저장)
        projectRepository.update(project);

    }

    public void removeMemberFromProject(Project project, User member, User adminUser) {
        if (adminUser == null || !adminUser.isAdmin()) {
            throw new SecurityException("Member removal permission is required.");
        }
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null.");
        }
        if (member == null) {
            throw new IllegalArgumentException("Member must not be null.");
        }

        project.getMembers().remove(member);
        projectRepository.update(project);
    }

    //  프로젝트에 이슈 추가 및 파일 업데이트
    public void addIssueToProject(Project project, Issue issue) {
        // 유효성 검증
        validateProject(project);

        if (issue == null) {
            throw new IllegalArgumentException("추가할 이슈 정보가 없습니다.");
        }

        // 1. 프로젝트 객체에 이슈 추가
        project.addIssue(issue);

        // 2. 프로젝트 파일(JSON) 업데이트
        projectRepository.update(project);
    }

    // 프로젝트 삭제 (Admin 전용)
    public void deleteProject(int projectId, User adminUser) {
        
        // 1. 권한 검증: Admin만 프로젝트를 삭제할 수 있음
        validateAdmin(adminUser);
        validateProjectId(projectId);

        // 2. Repository에서 삭제 수행 (Repository 내부에서 파일 saveToFile()이 호출되어야 함)
        projectRepository.delete(projectId);
        
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    //테스트용
    // 전체 프로젝트 목록 출력
    public void showProjects(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            System.out.println("❌ 등록된 프로젝트가 없습니다.");
            return;
        }

        System.out.println("\n===== [전체 프로젝트 목록] =====");

        for (Project project : projects) {
            // NullPointerException 방지를 위해 리스트 사이즈 체크
            int memberCount = (project.getMembers() != null) ? project.getMembers().size() : 0;
            int issueCount = (project.getIssues() != null) ? project.getIssues().size() : 0;

            // 프로젝트 정보 출력
            System.out.println("📌 [프로젝트 #" + project.getProjectId() + "] " + project.getName());
            System.out.println("   - 설명: " + project.getDescription());
            System.out.println("   - 참여 인원: " + memberCount + "명 | 등록된 이슈: " + issueCount + "개");
            System.out.println("   ------------------------------------------");
        }

        System.out.println("==========================================\n");
    }

    //helper
    private void validateAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new SecurityException("Admin 권한이 필요합니다.");
        }
    }

    private void validateProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("프로젝트 정보가 없습니다.");
        }
    }

    private void validateProjectId(int projectId) {
        if (projectId <= 0) {
            throw new IllegalArgumentException("Project ID must be a positive number.");
        }
    }

    private void validateMember(Project project, User user) {
        validateProject(project);
        if (user == null || !project.getMembers().contains(user)) {
            throw new SecurityException("해당 프로젝트의 멤버가 아닙니다.");
        }
    }   
}
