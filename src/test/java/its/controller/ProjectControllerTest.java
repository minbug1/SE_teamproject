package its.controller;

import its.model.AccountStatus;
import its.model.Project;
import its.model.UserRole;
import its.model.User;
import its.repository.MemoryProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectControllerTest {

    private MemoryProjectRepository projectRepository;
    private ProjectController projectController;

    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        projectRepository = new MemoryProjectRepository();
        projectController = new ProjectController(projectRepository);

        admin      = new User(1L, "admin", "1234", AccountStatus.ACTIVE, UserRole.ADMIN);
        normalUser = new User(2L, "dev1",  "1234", AccountStatus.ACTIVE, UserRole.DEVELOPER);
    }

    // ?? createProject ?????????????????????????????????????????????????????????

    @Test
    void createProjectShouldSucceedForAdmin() {
        Project project = projectController.createProject("MyProject", "desc", admin);

        assertNotNull(project);
        assertEquals("MyProject", project.getName());
        assertEquals("desc", project.getDescription());
        assertTrue(project.getProjectId() > 0);
        assertNotNull(projectRepository.findById(project.getProjectId()));
    }

    @Test
    void createProjectShouldFailWhenNotAdmin() {
        assertThrows(SecurityException.class, () ->
                projectController.createProject("X", "desc", normalUser));
    }

    @Test
    void createProjectShouldFailWhenAdminIsNull() {
        assertThrows(SecurityException.class, () ->
                projectController.createProject("X", "desc", null));
    }

    @Test
    void createProjectShouldFailWhenNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.createProject("   ", "desc", admin));
    }

    @Test
    void createProjectShouldFailWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.createProject(null, "desc", admin));
    }

    // ?? updateProject ?????????????????????????????????????????????????????????

    @Test
    void updateProjectShouldSucceedForAdmin() {
        Project project = projectController.createProject("Original", "desc", admin);
        project.setName("Updated");

        assertDoesNotThrow(() -> projectController.updateProject(project, admin));
        assertEquals("Updated", projectRepository.findById(project.getProjectId()).getName());
    }

    @Test
    void updateProjectShouldFailWhenNotAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.updateProject(project, normalUser));
    }

    @Test
    void updateProjectShouldFailWhenAdminIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.updateProject(project, null));
    }

    @Test
    void updateProjectShouldFailWhenProjectIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.updateProject(null, admin));
    }

    @Test
    void updateProjectShouldFailWhenNameIsEmpty() {
        Project project = projectController.createProject("P", "d", admin);
        project.setName("   ");
        assertThrows(IllegalArgumentException.class, () ->
                projectController.updateProject(project, admin));
    }

    // ?? addMemberToProject ????????????????????????????????????????????????????

    @Test
    void addMemberShouldSucceedForAdmin() {
        Project project = projectController.createProject("P", "d", admin);

        projectController.addMemberToProject(project, normalUser, admin);

        assertTrue(project.getMembers().contains(normalUser));
    }

    @Test
    void addMemberShouldFailWhenNotAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.addMemberToProject(project, normalUser, normalUser));
    }

    @Test
    void addMemberShouldFailWhenAdminIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.addMemberToProject(project, normalUser, null));
    }

    @Test
    void addMemberShouldFailWhenProjectIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.addMemberToProject(null, normalUser, admin));
    }

    @Test
    void addMemberShouldFailWhenMemberIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(IllegalArgumentException.class, () ->
                projectController.addMemberToProject(project, null, admin));
    }

    @Test
    void addMemberShouldNotAddDuplicates() {
        Project project = projectController.createProject("P", "d", admin);

        projectController.addMemberToProject(project, normalUser, admin);
        projectController.addMemberToProject(project, normalUser, admin); // 以묐났

        assertEquals(1, project.getMembers().size());
    }

    // ?? removeMemberFromProject ???????????????????????????????????????????????

    @Test
    void removeMemberShouldSucceedForAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        projectController.addMemberToProject(project, normalUser, admin);

        projectController.removeMemberFromProject(project, normalUser, admin);

        assertFalse(project.getMembers().contains(normalUser));
    }

    @Test
    void removeMemberShouldFailWhenNotAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.removeMemberFromProject(project, normalUser, normalUser));
    }

    @Test
    void removeMemberShouldFailWhenAdminIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.removeMemberFromProject(project, normalUser, null));
    }

    @Test
    void removeMemberShouldFailWhenProjectIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.removeMemberFromProject(null, normalUser, admin));
    }

    @Test
    void removeMemberShouldFailWhenMemberIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(IllegalArgumentException.class, () ->
                projectController.removeMemberFromProject(project, null, admin));
    }

    // ?? deleteProject ?????????????????????????????????????????????????????????

    @Test
    void deleteProjectShouldSucceedForAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        int id = project.getProjectId();

        projectController.deleteProject(id, admin);

        assertNull(projectRepository.findById(id));
    }

    @Test
    void deleteProjectShouldFailWhenNotAdmin() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.deleteProject(project.getProjectId(), normalUser));
    }

    @Test
    void deleteProjectShouldFailWhenAdminIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(SecurityException.class, () ->
                projectController.deleteProject(project.getProjectId(), null));
    }

    @Test
    void deleteProjectShouldFailWhenIdIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.deleteProject(0, admin));
    }

    @Test
    void deleteProjectShouldFailWhenIdIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.deleteProject(-1, admin));
    }

    // ?? getAllProjects ?????????????????????????????????????????????????????????

    @Test
    void getAllProjectsShouldReturnEmptyWhenNoProjects() {
        List<Project> all = projectController.getAllProjects();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    void getAllProjectsShouldReturnAllProjects() {
        projectController.createProject("A", "d", admin);
        projectController.createProject("B", "d", admin);

        List<Project> all = projectController.getAllProjects();
        assertEquals(2, all.size());
    }

    // ?? addIssueToProject ?????????????????????????????????????????????????????

    @Test
    void addIssueToProjectShouldFailWhenProjectIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                projectController.addIssueToProject(null, null));
    }

    @Test
    void addIssueToProjectShouldFailWhenIssueIsNull() {
        Project project = projectController.createProject("P", "d", admin);
        assertThrows(IllegalArgumentException.class, () ->
                projectController.addIssueToProject(project, null));
    }
}