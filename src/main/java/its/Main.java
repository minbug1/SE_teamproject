package its;

import its.controller.AuthController;
import its.controller.IssueController;
import its.controller.ProjectController;
import its.controller.UserController;
import its.model.AccountStatus;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.Role;
import its.model.User;
import its.repository.FileIssueRepository;
import its.repository.FileProjectRepository;
import its.repository.FileUserRepository;

import java.io.File;
import java.nio.file.Files;

public class Main {

    // 테스트용 파일 경로 (실제 운영 데이터와 분리)
    static final String USER_FILE    = "data/test_users.json";
    static final String ISSUE_FILE   = "data/test_issues.json";
    static final String PROJECT_FILE = "data/test_projects.json";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   저장/복원 통합 테스트 시작");
        System.out.println("========================================\n");

        // ── 테스트 파일 초기화 ──────────────────────
        deleteIfExists(USER_FILE);
        deleteIfExists(ISSUE_FILE);
        deleteIfExists(PROJECT_FILE);

        // ── Repository / Controller 세팅 ────────────
        FileUserRepository    userRepo    = new FileUserRepository(USER_FILE);
        FileIssueRepository   issueRepo   = new FileIssueRepository(ISSUE_FILE, userRepo);
        FileProjectRepository projectRepo = new FileProjectRepository(PROJECT_FILE, userRepo, issueRepo);

        AuthController    authController    = new AuthController(userRepo);
        UserController    userController    = new UserController(userRepo);
        IssueController   issueController   = new IssueController(issueRepo);
        ProjectController projectController = new ProjectController(projectRepo);

        try {

            // =====================================================
            // STEP 1. 유저 생성 및 저장
            // =====================================================
            System.out.println("▶ [STEP 1] 유저 생성 및 JSON 저장");

            // Admin은 저장소에 직접 저장 (회원가입 절차 없이 시스템 초기 계정 생성)
            User admin = new User(userRepo.generateUserId(), "admin", "admin1234",
                    AccountStatus.ACTIVE, Role.ADMIN);
            userRepo.save(admin);

            // 나머지 유저는 회원가입 → 관리자 승인 흐름
            User plRaw     = authController.register("iampl",     "pass1234");
            User devRaw    = authController.register("iamdev",    "pass1234");
            User testerRaw = authController.register("iamtester", "pass1234");

            userController.approveUser(admin, plRaw.getUserId(),     Role.PL);
            userController.approveUser(admin, devRaw.getUserId(),    Role.DEVELOPER);
            userController.approveUser(admin, testerRaw.getUserId(), Role.TESTER);

            // 승인 후 최신 상태로 다시 조회
            User pl     = userRepo.findByUserId(plRaw.getUserId());
            User dev    = userRepo.findByUserId(devRaw.getUserId());
            User tester = userRepo.findByUserId(testerRaw.getUserId());

            System.out.println("  저장된 유저 목록:");
            for (User u : userRepo.findAll()) {
                System.out.println("    - " + u.getLoginId() + " | " + u.getRole() + " | " + u.getAccountStatus());
            }

            // =====================================================
            // STEP 2. 프로젝트 생성 및 멤버 추가 → 저장
            // =====================================================
            System.out.println("\n▶ [STEP 2] 프로젝트 생성 및 JSON 저장");

            Project project = projectController.createProject("테스트 프로젝트", "저장 테스트용", admin);
            projectController.addMemberToProject(project, pl,     admin);
            projectController.addMemberToProject(project, dev,    admin);
            projectController.addMemberToProject(project, tester, admin);
            projectRepo.update(project);

            System.out.println("  저장된 프로젝트: " + project.getName()
                    + " | 멤버 수: " + project.getMembers().size());

            // =====================================================
            // STEP 3. 이슈 등록 → ASSIGNED → FIXED → RESOLVED → CLOSED
            // =====================================================
            System.out.println("\n▶ [STEP 3] 이슈 전체 흐름 테스트 및 JSON 저장");

            Issue issue = issueController.reportIssue(
                    project,
                    "결제 완료 후 영수증 출력 에러",
                    "IE 환경에서 영수증 팝업이 뜨지 않습니다.",
                    tester,
                    Priority.MAJOR,
                    "빠른 확인이 필요합니다."
            );

            issueController.assignIssue(project, issue.getIssueId(), dev, pl, "확인해주세요.");
            projectController.addIssueToProject(project, issue);
            issueController.fixIssue(project, issue.getIssueId(), "팝업 차단 우회 처리 완료.", dev);
            issueController.verifyIssue(project, issue.getIssueId(), "정상 작동 확인.", tester, true);
            issueController.closeIssue(project, issue.getIssueId(), "이슈 닫습니다.", pl);

            // 프로젝트에 이슈가 추가됐으므로 저장 내용 갱신
            projectRepo.update(project);

            System.out.println("  이슈 최종 상태: " + issueRepo.findById(issue.getIssueId()).getStatus());
            System.out.println("  이슈 코멘트 수: " + issueRepo.findById(issue.getIssueId()).getComments().size());

            // =====================================================
            // STEP 4. 프로그램 재시작 시뮬레이션 (Repository 재생성)
            // =====================================================
            System.out.println("\n▶ [STEP 4] Repository 재생성 후 파일에서 데이터 복원 확인");

            FileUserRepository    userRepo2    = new FileUserRepository(USER_FILE);
            FileIssueRepository   issueRepo2   = new FileIssueRepository(ISSUE_FILE, userRepo2);
            FileProjectRepository projectRepo2 = new FileProjectRepository(PROJECT_FILE, userRepo2, issueRepo2);

            // 유저 복원 확인
            System.out.println("\n  [유저 복원]");
            for (User u : userRepo2.findAll()) {
                System.out.println("    - " + u.getLoginId() + " | " + u.getRole() + " | " + u.getAccountStatus());
            }

            // 이슈 복원 확인
            System.out.println("\n  [이슈 복원]");
            for (Issue i : issueRepo2.findAll()) {
                System.out.println("    - [#" + i.getIssueId() + "] " + i.getTitle()
                        + " | 상태: " + i.getStatus()
                        + " | Reporter: " + (i.getReporter() != null ? i.getReporter().getLoginId() : "null")
                        + " | Assignee: " + (i.getAssignee() != null ? i.getAssignee().getLoginId() : "null")
                        + " | Fixer: "    + (i.getFixer()    != null ? i.getFixer().getLoginId()    : "null"));
                System.out.println("      코멘트 수: " + i.getComments().size());
                for (var c : i.getComments()) {
                    System.out.println("        └ [" + c.getAuthor().getLoginId() + "] " + c.getContent());
                }
            }

            // 프로젝트 복원 확인
            System.out.println("\n  [프로젝트 복원]");
            for (Project p : projectRepo2.findAll()) {
                System.out.println("    - " + p.getName()
                        + " | 멤버 수: " + p.getMembers().size()
                        + " | 이슈 수: " + p.getIssues().size());
            }

            // =====================================================
            // STEP 5. JSON 파일 내용 출력
            // =====================================================
            System.out.println("\n▶ [STEP 5] 저장된 JSON 파일 내용 확인");
            printFile(USER_FILE);
            printFile(ISSUE_FILE);
            printFile(PROJECT_FILE);

            System.out.println("\n========================================");
            System.out.println("   모든 테스트 완료 ✅");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ 테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteIfExists(String path) {
        File f = new File(path);
        if (f.exists()) f.delete();
    }

    private static void printFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("[" + path + "] 파일 없음");
                return;
            }
            System.out.println("---------- " + path + " ----------");
            System.out.println(new String(Files.readAllBytes(f.toPath())));
        } catch (Exception e) {
            System.err.println("파일 읽기 실패: " + path);
        }
    }
}