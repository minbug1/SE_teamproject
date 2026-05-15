package its;

import its.controller.IssueController;
import its.controller.ProjectController;
import its.model.Admin;
import its.model.Developer;
import its.model.Issue;
import its.model.PL;
import its.model.Priority;
import its.model.Project;
import its.model.Tester;

public class Main {
    //테스트용
    public static void main(String[] args) {
        System.out.println("시작\n");
        Admin admin = new Admin(234L, "asdf", "erwe");
        PL pl = new PL(123L, "iampl", "3434");
        Developer dev = new Developer(124L, "iamdev", "3434");
        Tester tester = new Tester(124L, "iamtester", "3434");


        ProjectController pController = new ProjectController();
        IssueController iController = new IssueController();

        System.out.println("▶ [STEP 1] Admin이 프로젝트를 생성하고 멤버를 추가합니다.");
        Project projectA = pController.createProject("프로젝트 베인", "베인", admin);
        pController.addMemberToProject(projectA, pl, admin);
        pController.addMemberToProject(projectA, dev, admin);
        pController.addMemberToProject(projectA, tester, admin);

        System.out.println("✔️ 프로젝트 생성 완료: " + projectA.getName());
        System.out.println("✔️ 현재 참여 멤버 수: " + projectA.getMembers().size() + "명\n");

        System.out.println("▶ [STEP 2] Tester가 신규 이슈를 등록합니다.");

        Issue issue = iController.reportIssue(
                projectA, 
                "결제 완료 후 영수증 출력 에러", 
                "IE 환경에서 영수증 팝업이 뜨지 않습니다.", 
                tester, 
                Priority.MAJOR, 
                "빠른 확인이 필요합니다."
        ); 
        issue.printIssueInfo();

        System.out.println("▶ [STEP 3] PL이 해당 이슈를 Developer에게 할당합니다.");
        iController.assignIssue(
                projectA, 
                issue.getIssueId(), 
                dev, 
                pl, 
                "영수증 출력 로직 확인해주세요."
        );
        issue.printIssueInfo();

        System.out.println("▶ [STEP 4] Developer가 버그를 수정하고 상태를 FIXED로 변경합니다.");
        iController.updateState(
                projectA, 
                issue.getIssueId(), 
                "팝업 차단 로직 우회 처리 완료했습니다.", 
                dev
        );
        issue.printIssueInfo();

        System.out.println("▶ [STEP 5] Tester가 수정 사항을 테스트하고 RESOLVED 처리합니다.");
        // 파라미터 boolean isResolved = true (검증 성공)
        iController.updateState(
                projectA, 
                issue.getIssueId(), 
                "테스트 완료. IE에서 정상 작동합니다.", 
                tester, 
                true 
        );
        issue.printIssueInfo();

        System.out.println("▶ [STEP 6] PL이 이슈를 최종적으로 CLOSED 처리합니다.");
        iController.updateState(
                projectA, 
                issue.getIssueId(), 
                "수고하셨습니다. 이슈 닫습니다.", 
                pl
        );
        issue.printIssueInfo();

        System.out.println("hello");

        iController.showIssues(projectA, null);
    }
}
