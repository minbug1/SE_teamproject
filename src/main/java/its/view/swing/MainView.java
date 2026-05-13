package its.view.swing;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import its.controller.IssueController;
import its.model.User;

public class MainView extends JFrame {

    private JTable issueTable;
    private DefaultTableModel tableModel;
    private IssueController issueController;
    private User currentUser;

    // 컬럼 인덱스 상수로 관리
    private static final int COL_ID       = 0;
    private static final int COL_NAME     = 1;
    private static final int COL_ACTION   = 2;
    private static final int COL_PRIORITY = 3;
    private static final int COL_STATUS   = 4;
    private static final int COL_REPORTER = 5;
    private static final int COL_ASSIGNEE = 6;

    private static final String[] COLUMNS = {
        "ID", "Issue Name", "Action",
        "Priority", "Status", "Reporter", "Assignee"
    };

    public MainView(IssueController controller, User currentUser) {
        this.issueController = controller;
        this.currentUser = currentUser;
        initUI();
    }

    private void initUI() {
        setTitle("Issue Tracker");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
    }

    // ── 상단 패널 ──────────────────────────────
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Issue Tracker");
        JButton logoutBtn = new JButton("로그아웃");
        JButton reportBtn = new JButton("+ 이슈 등록");

        logoutBtn.addActionListener(e -> onLogout());
        reportBtn.addActionListener(e -> onReportIssue());

        JPanel rightPanel = new JPanel();
        rightPanel.add(reportBtn);
        rightPanel.add(logoutBtn);

        panel.add(title, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    // ── 테이블 패널 ────────────────────────────
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 테이블 모델 (편집 불가)
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COL_ACTION; // Action 컬럼만 클릭 가능
            }
        };

        issueTable = new JTable(tableModel);
        issueTable.setRowHeight(36);
        issueTable.getTableHeader().setReorderingAllowed(false);

        // Action 컬럼 렌더러/에디터 설정
        issueTable.getColumn("Action").setCellRenderer(new ActionButtonRenderer());
        issueTable.getColumn("Action").setCellEditor(new ActionButtonEditor(currentUser));

        // 컬럼 너비 설정
        issueTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(40);
        issueTable.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
        issueTable.getColumnModel().getColumn(COL_ACTION).setPreferredWidth(100);
        issueTable.getColumnModel().getColumn(COL_PRIORITY).setPreferredWidth(80);
        issueTable.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_REPORTER).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(COL_ASSIGNEE).setPreferredWidth(90);

        // 더미 데이터 (FileIssueRepository 연결 전 테스트용)
        loadDummyData();

        panel.add(new JScrollPane(issueTable), BorderLayout.CENTER);
        return panel;
    }

    // ── 더미 데이터 ────────────────────────────
    private void loadDummyData() {
        tableModel.addRow(new Object[]{
            1, "로그인 버튼 클릭 시 오류", "액션", "HIGH", "NEW", "tester1", "-"
        });
        tableModel.addRow(new Object[]{
            2, "회원가입 이메일 중복 체크 안됨", "액션", "MEDIUM", "ASSIGNED", "tester2", "dev1"
        });
        tableModel.addRow(new Object[]{
            3, "마이페이지 이미지 업로드 실패", "액션", "LOW", "FIXED", "tester1", "dev3"
        });
    }

    // ── 이벤트 핸들러 ──────────────────────────
    private void onReportIssue() {
        // ReportIssueView 연결 (추후 구현)
        JOptionPane.showMessageDialog(this, "이슈 등록 화면 (추후 구현)");
        // ReportIssueView dialog = new ReportIssueView(this, issueController, currentUser);
        // dialog.setVisible(true);
        // refreshTable(); // 등록 후 목록 갱신
    }

    private void onLogout() {
        int confirm = JOptionPane.showConfirmDialog(
            this, "로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            // new LoginView().setVisible(true); // 추후 연결
        }
    }

    // ── 테이블 갱신 ────────────────────────────
    public void refreshTable() {
        tableModel.setRowCount(0); // 기존 데이터 초기화
        // issueController.getAllIssues() 로 데이터 로드 (추후 연결)
        // for (Issue issue : issues) {
        //     tableModel.addRow(new Object[]{
        //         issue.getId(), issue.getTitle(), "액션",
        //         issue.getPriority(), issue.getStatus(),
        //         issue.getReporter().getUsername(),
        //         issue.getAssignee() != null ? issue.getAssignee().getUsername() : "-"
        //     });
        // }
    }

    public void open() {
        setVisible(true);
    }
}
