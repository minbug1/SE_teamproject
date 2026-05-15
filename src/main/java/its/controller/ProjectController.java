package its.controller;

import its.model.Project;
import its.model.User;

public class ProjectController {

    //admin이 프로젝트 생성
    public Project createProject(String name, String description, User adminUser) {
        
        // Admin 권한 검증 (User 클래스의 isAdmin() 활용)
        if (adminUser == null || !adminUser.isAdmin()) {
            throw new SecurityException("프로젝트 생성 권한이 없습니다. Admin만 생성 가능합니다.");
        }

        // 유효성 검증
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("프로젝트 이름은 필수입니다.");
        }

        // 프로젝트 객체 생성
        // projectId는 pojectrepository 만드록 수정 해야됨
        int newProjectId = 1; 
        Project newProject = new Project(newProjectId, name, description);

        //아직 projectrepository 안 만듦 

        return newProject;
    }

    //멤버 추가
    public void addMemberToProject(Project project, User newMember, User adminUser) {
        
        // Admin 권한 검증
        if (adminUser == null || !adminUser.isAdmin()) {
            throw new SecurityException("멤버 추가 권한이 없습니다. Admin만 추가 가능합니다.");
        }

        // 유효성 검증
        if (project == null) {
            throw new IllegalArgumentException("대상 프로젝트 정보가 없습니다.");
        }
        if (newMember == null) {
            throw new IllegalArgumentException("추가할 유저 정보가 없습니다.");
        }

        // 프로젝트에 멤버 추가
        project.addMember(newMember);

    }
}