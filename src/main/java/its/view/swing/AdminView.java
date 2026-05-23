package its.view.swing;

import its.controller.IssueController;
import its.model.AccountStatus;
import its.model.Project;
import its.model.Role;
import its.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminView — 프로젝트/멤버/Unassigned 관리 화면
 *
 * 진입: LoginView 에서 user.isAdmin() == true 일 때
 *       new AdminView(issueController, adminUser, projects).setVisible(true);
 *
 * 레이아웃:
 *   [툴바]
 *   [사이드바 | 우측 카드 패널]
 *        프로젝트 목록          PROJECT  카드: 멤버 테이블 + 추가/제거
 *        ── Unassigned ──       UNASSIGNED 카드: 미배정 유저 테이블
 *   [+ 프로젝트 추가]
 */
public class AdminView extends JFrame {

    // ── 색상 팔레트 ───────────────────────────────────────
    private static final Color BG_DARK     = new Color(0x111827);
    private static final Color BG_PANEL    = new Color(0x1F2937);
    private static final Color BG_SIDEBAR  = new Color(0x0D1117);
    private static final Color BG_INPUT    = new Color(0x374151);
    private static final Color ACCENT      = new Color(0xE94560);
    private static final Color FG_WHITE    = new Color(0xF9FAFB);
    private static final Color FG_LABEL    = new Color(0x9CA3AF);
    private static final Color FG_HINT     = new Color(0x6B7280);
    private static final Color SEL_BG      = new Color(0x374151);
    private static final Color DIVIDER     = new Color(0x374151);
    private static final Color C_ADMIN     = new Color(0xE94560);
    private static final Color C_PL        = new Color(0x8B5CF6);
    private static final Color C_DEV       = new Color(0x3B82F6);
    private static final Color C_TESTER    = new Color(0x10B981);
    private static final Color C_UNASSIGN  = new Color(0xF59E0B);
    private static final Color C_PENDING   = new Color(0xF59E0B);
    private static final Color C_ACTIVE    = new Color(0x10B981);
    private static final Color C_REJECTED  = new Color(0xEF4444);
    private static final Color C_DISABLED  = new Color(0x6B7280);

    private final User             adminUser;
    private final IssueController  issueController;
    private final List<Project>    projects;
    private final List<User>       allUsers;

    // ── 사이드바 ─────────────────────────────────────────
    private DefaultListModel<String> sidebarModel;
    private JList<String>            sidebarList;

    // ── 우측 카드 ─────────────────────────────────────────
    private JPanel    rightCards;
    private CardLayout cardLayout;

    // PROJECT 카드
    private JLabel            lblProjectTitle;
    private JLabel            lblProjectDesc;
    private DefaultTableModel memberTableModel;
    private JTable            memberTable;

    // UNASSIGNED 카드
    private DefaultTableModel unassignedTableModel;
    private JTable            unassignedTable;

    // 현재 선택된 프로젝트
    private Project selectedProject = null;

    // ── 사이드바 항목 구분 상수 ───────────────────────────
    private static final String UNASSIGNED_KEY = "__UNASSIGNED__";

    // ════════════════════════════════════════════════════════
    //  생성자
    // ════════════════════════════════════════════════════════
    /**
     * @param issueController  공유 컨트롤러
     * @param adminUser        로그인한 Admin User 객체
     * @param projects         앱 전체에서 공유되는 프로젝트 리스트 (가변)
     * @param allUsers         앱 전체에서 공유되는 유저 리스트   (가변)
     */
    public AdminView(IssueController issueController,
                     User adminUser,
                     List<Project> projects,
                     List<User> allUsers) {
        this.issueController = issueController;
        this.adminUser       = adminUser;
        this.projects        = projects;
        this.allUsers        = allUsers;

        setTitle("Issue Tracker — Admin Panel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 620);
        setLocationRelativeTo(null);
        buildUI();
    }

    // ════════════════════════════════════════════════════════
    //  UI 조립
    // ════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        setContentPane(root);

        root.add(buildToolbar(),   BorderLayout.NORTH);
        root.add(buildSidebar(),   BorderLayout.WEST);
        root.add(buildRightArea(), BorderLayout.CENTER);

        refreshSidebar();

        // 첫 번째 프로젝트 자동 선택
        if (!projects.isEmpty()) {
            sidebarList.setSelectedIndex(0);
        }
    }

    // ── 툴바 ─────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SIDEBAR);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("🐛  Issue Tracker  —  Admin Panel");
        title.setFont(new Font("Monospaced", Font.BOLD, 16));
        title.setForeground(ACCENT);
        bar.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        // 로그인 유저 배지
        JLabel badge = new JLabel("  " + adminUser.getLoginId() + "  ");
        badge.setFont(new Font("Monospaced", Font.BOLD, 11));
        badge.setForeground(Color.WHITE);
        badge.setBackground(C_ADMIN);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        right.add(badge);

        JButton btnLogout = makeBtn("로그아웃", BG_INPUT);
        btnLogout.addActionListener(e -> onLogout());
        right.add(btnLogout);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── 사이드바 ─────────────────────────────────────────
    private JComponent buildSidebar() {
        sidebarModel = new DefaultListModel<>();
        sidebarList  = new JList<>(sidebarModel);
        sidebarList.setBackground(BG_SIDEBAR);
        sidebarList.setForeground(FG_WHITE);
        sidebarList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sidebarList.setSelectionBackground(SEL_BG);
        sidebarList.setSelectionForeground(FG_WHITE);
        sidebarList.setFixedCellHeight(42);
        sidebarList.setBorder(BorderFactory.createEmptyBorder());
        sidebarList.setCellRenderer(new SidebarRenderer());
        sidebarList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSidebarSelect(sidebarList.getSelectedIndex());
        });

        // 사이드바 외부 패널
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SIDEBAR);
        panel.setPreferredSize(new Dimension(210, 0));

        // 헤더
        JLabel header = new JLabel("  📁  Projects");
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setForeground(FG_LABEL);
        header.setBorder(new EmptyBorder(12, 8, 8, 8));
        header.setOpaque(true);
        header.setBackground(BG_SIDEBAR);
        panel.add(header, BorderLayout.NORTH);

        // 목록 스크롤
        JScrollPane sp = new JScrollPane(sidebarList);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(BG_SIDEBAR);
        sp.getViewport().setBackground(BG_SIDEBAR);
        panel.add(sp, BorderLayout.CENTER);

        // + 프로젝트 추가 버튼
        JButton btnAdd = makeBtn("+ 프로젝트 추가", new Color(0x1E3A5F));
        btnAdd.setPreferredSize(new Dimension(0, 38));
        btnAdd.addActionListener(e -> doAddProject());
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setBackground(BG_SIDEBAR);
        btnWrap.setBorder(new EmptyBorder(6, 8, 8, 8));
        btnWrap.add(btnAdd);
        panel.add(btnWrap, BorderLayout.SOUTH);

        // 오른쪽 경계선
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_SIDEBAR);
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DIVIDER));
        return wrapper;
    }

    // ── 우측 카드 영역 ───────────────────────────────────
    private JPanel buildRightArea() {
        cardLayout = new CardLayout();
        rightCards = new JPanel(cardLayout);
        rightCards.setBackground(BG_PANEL);

        rightCards.add(buildProjectCard(),    "PROJECT");
        rightCards.add(buildUnassignedCard(), "UNASSIGNED");
        rightCards.add(buildEmptyCard(),      "EMPTY");

        return rightCards;
    }

    // ════════════════════════════════════════════════════════
    //  PROJECT 카드
    // ════════════════════════════════════════════════════════
    private JPanel buildProjectCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_PANEL);

        // 헤더
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(18, 22, 12, 22));

        lblProjectTitle = new JLabel("프로젝트");
        lblProjectTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblProjectTitle.setForeground(FG_WHITE);

        lblProjectDesc = new JLabel(" ");
        lblProjectDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblProjectDesc.setForeground(FG_HINT);

        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.add(lblProjectTitle, BorderLayout.CENTER);
        titleBox.add(lblProjectDesc, BorderLayout.SOUTH);
        header.add(titleBox, BorderLayout.WEST);

        // 헤더 버튼들
        JPanel headerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerBtns.setOpaque(false);

        JButton btnEdit   = makeBtn("✏ 이름 변경", BG_INPUT);
        JButton btnDelete = makeBtn("🗑 프로젝트 삭제", new Color(0x7F1D1D));
        btnEdit.addActionListener(e   -> doEditProject());
        btnDelete.addActionListener(e -> doDeleteProject());
        headerBtns.add(btnEdit);
        headerBtns.add(btnDelete);
        header.add(headerBtns, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // 멤버 테이블
        memberTableModel = new DefaultTableModel(
                new String[]{"Username", "Role", "Account Status", "액션"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        memberTable = buildStyledTable(memberTableModel);
        memberTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        memberTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        memberTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        memberTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        memberTable.getColumnModel().getColumn(1).setCellRenderer(roleBadgeRenderer());
        memberTable.getColumnModel().getColumn(2).setCellRenderer(statusBadgeRenderer());
        memberTable.getColumnModel().getColumn(3).setCellRenderer(btnRenderer("제거", new Color(0x7F1D1D)));
        memberTable.getColumnModel().getColumn(3).setCellEditor(new RemoveMemberEditor());

        JScrollPane sp = new JScrollPane(memberTable);
        styleScrollPane(sp);

        JLabel sec = makeSectionLabel("멤버 목록");
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(BG_PANEL);
        center.setBorder(new EmptyBorder(0, 22, 0, 22));
        center.add(sec, BorderLayout.NORTH);
        center.add(sp,  BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        // 하단: 멤버 추가
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        bottom.setBackground(BG_PANEL);
        bottom.setBorder(new EmptyBorder(0, 22, 16, 22));
        JButton btnAddMember = makeBtn("+ 멤버 추가", new Color(0x065F46));
        btnAddMember.addActionListener(e -> doAddMember());
        bottom.add(btnAddMember);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    // ════════════════════════════════════════════════════════
    //  UNASSIGNED 카드
    // ════════════════════════════════════════════════════════
    private JPanel buildUnassignedCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_PANEL);

        // 헤더
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(18, 22, 12, 22));

        JLabel title = new JLabel("⚠  Unassigned / Pending Users");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(C_UNASSIGN);
        header.add(title, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        // 테이블
        unassignedTableModel = new DefaultTableModel(
                new String[]{"Username", "Role", "Account Status", "액션"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        unassignedTable = buildStyledTable(unassignedTableModel);
        unassignedTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        unassignedTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        unassignedTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        unassignedTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        unassignedTable.getColumnModel().getColumn(1).setCellRenderer(roleBadgeRenderer());
        unassignedTable.getColumnModel().getColumn(2).setCellRenderer(statusBadgeRenderer());
        unassignedTable.getColumnModel().getColumn(3).setCellRenderer(btnRenderer("프로젝트 배정", new Color(0x1E40AF)));
        unassignedTable.getColumnModel().getColumn(3).setCellEditor(new AssignUserEditor());

        JScrollPane sp = new JScrollPane(unassignedTable);
        styleScrollPane(sp);

        JLabel sec = makeSectionLabel("어떤 프로젝트에도 배정되지 않은 계정  (읽기 전용으로 모든 프로젝트 열람 가능)");
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(BG_PANEL);
        center.setBorder(new EmptyBorder(0, 22, 20, 22));
        center.add(sec, BorderLayout.NORTH);
        center.add(sp,  BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    // ── 빈 카드 ──────────────────────────────────────────
    private JPanel buildEmptyCard() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PANEL);
        JLabel l = new JLabel("좌측에서 프로젝트를 선택하세요");
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        l.setForeground(FG_HINT);
        p.add(l);
        return p;
    }

    // ════════════════════════════════════════════════════════
    //  사이드바 선택 → 우측 갱신
    // ════════════════════════════════════════════════════════
    private void onSidebarSelect(int idx) {
        if (idx < 0) { cardLayout.show(rightCards, "EMPTY"); return; }

        if (idx < projects.size()) {
            selectedProject = projects.get(idx);
            refreshProjectCard();
            cardLayout.show(rightCards, "PROJECT");
        } else {
            // Unassigned 항목
            selectedProject = null;
            refreshUnassignedCard();
            cardLayout.show(rightCards, "UNASSIGNED");
        }
    }

    // ════════════════════════════════════════════════════════
    //  갱신 메서드
    // ════════════════════════════════════════════════════════
    private void refreshSidebar() {
        int prevSel = sidebarList.getSelectedIndex();
        sidebarModel.clear();
        for (Project p : projects)
            sidebarModel.addElement(p.getName());
        sidebarModel.addElement(UNASSIGNED_KEY);

        int total = sidebarModel.getSize();
        if (prevSel >= 0 && prevSel < total)
            sidebarList.setSelectedIndex(prevSel);
        else if (total > 0)
            sidebarList.setSelectedIndex(0);
    }

    private void refreshProjectCard() {
        if (selectedProject == null) return;

        lblProjectTitle.setText("📁  " + selectedProject.getName()
                + "  (" + selectedProject.getMembers().size() + "명)");
        String desc = selectedProject.getDescription();
        lblProjectDesc.setText(desc != null && !desc.isBlank() ? desc : " ");

        memberTableModel.setRowCount(0);
        for (User u : selectedProject.getMembers()) {
            memberTableModel.addRow(new Object[]{
                u.getLoginId(),
                u.getRole().name(),
                u.getAccountStatus().name(),
                "제거"
            });
        }
    }

    private void refreshUnassignedCard() {
        unassignedTableModel.setRowCount(0);
        for (User u : getUnassignedUsers()) {
            unassignedTableModel.addRow(new Object[]{
                u.getLoginId(),
                u.getRole().name(),
                u.getAccountStatus().name(),
                "프로젝트 배정"
            });
        }
    }

    /** 어떤 프로젝트 멤버에도 없는 유저 (Admin 제외) */
    private List<User> getUnassignedUsers() {
        List<User> result = new ArrayList<>();
        for (User u : allUsers) {
            if (u.isAdmin()) continue;
            boolean assigned = false;
            for (Project p : projects) {
                if (p.getMembers().contains(u)) { assigned = true; break; }
            }
            if (!assigned) result.add(u);
        }
        return result;
    }

    // ════════════════════════════════════════════════════════
    //  액션: 프로젝트 CRUD
    // ════════════════════════════════════════════════════════
    private void doAddProject() {
        JTextField tfName = new JTextField();
        JTextField tfDesc = new JTextField();
        styleInputField(tfName);
        styleInputField(tfDesc);

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.setBackground(BG_PANEL);
        form.add(makeFormLabel("프로젝트 이름 *")); form.add(tfName);
        form.add(makeFormLabel("설명 (선택)"));     form.add(tfDesc);

        int r = JOptionPane.showConfirmDialog(this, form,
                "프로젝트 추가", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showWarn("프로젝트 이름은 필수입니다."); return;
        }
        for (Project p : projects)
            if (p.getName().equals(name)) { showWarn("이미 존재하는 이름입니다."); return; }

        int newId = projects.stream().mapToInt(Project::getProjectId).max().orElse(0) + 1;
        projects.add(new Project(newId, name, tfDesc.getText().trim()));
        refreshSidebar();
        sidebarList.setSelectedIndex(projects.size() - 1);
    }

    private void doEditProject() {
        if (selectedProject == null) return;
        String newName = JOptionPane.showInputDialog(this,
                "새 프로젝트 이름:", selectedProject.getName());
        if (newName == null || newName.isBlank()) return;
        selectedProject.setName(newName.trim());
        refreshSidebar();
        refreshProjectCard();
    }

    private void doDeleteProject() {
        if (selectedProject == null) return;
        int r = JOptionPane.showConfirmDialog(this,
                "'" + selectedProject.getName() + "' 을 삭제하시겠습니까?\n"
                + "멤버 배정과 이슈 연결이 모두 해제됩니다.",
                "프로젝트 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        projects.remove(selectedProject);
        selectedProject = null;
        refreshSidebar();
        cardLayout.show(rightCards, "EMPTY");
    }

    // ════════════════════════════════════════════════════════
    //  액션: 멤버 추가 / 제거
    // ════════════════════════════════════════════════════════
    private void doAddMember() {
        if (selectedProject == null) return;
        List<User> candidates = getUnassignedUsers();
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "추가 가능한 Unassigned 유저가 없습니다.", "알림",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // loginId 배열로 선택
        String[] ids = candidates.stream().map(User::getLoginId).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(this,
                "프로젝트에 추가할 유저:", "멤버 추가",
                JOptionPane.PLAIN_MESSAGE, null, ids, ids[0]);
        if (chosen == null) return;

        candidates.stream()
                .filter(u -> u.getLoginId().equals(chosen))
                .findFirst()
                .ifPresent(u -> {
                    selectedProject.addMember(u);
                    refreshProjectCard();
                    refreshUnassignedCard();
                    refreshSidebar();
                    sidebarList.setSelectedIndex(projects.indexOf(selectedProject));
                });
    }

    private void doRemoveMember(int row) {
        if (selectedProject == null || row < 0) return;
        List<User> members = selectedProject.getMembers();
        if (row >= members.size()) return;

        User target = members.get(row);
        int r = JOptionPane.showConfirmDialog(this,
                "'" + target.getLoginId() + "' 을 프로젝트에서 제거하시겠습니까?",
                "멤버 제거", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        members.remove(target);
        refreshProjectCard();
        refreshUnassignedCard();
        refreshSidebar();
        sidebarList.setSelectedIndex(projects.indexOf(selectedProject));
    }

    // ════════════════════════════════════════════════════════
    //  액션: Unassigned → 프로젝트 배정
    // ════════════════════════════════════════════════════════
    private void doAssignToProject(int row) {
        List<User> unassigned = getUnassignedUsers();
        if (row < 0 || row >= unassigned.size()) return;
        User target = unassigned.get(row);

        if (projects.isEmpty()) {
            showWarn("배정 가능한 프로젝트가 없습니다."); return;
        }
        String[] names = projects.stream().map(Project::getName).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(this,
                "'" + target.getLoginId() + "' 을 배정할 프로젝트:",
                "프로젝트 배정", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null) return;

        projects.stream()
                .filter(p -> p.getName().equals(chosen))
                .findFirst()
                .ifPresent(p -> {
                    p.addMember(target);
                    refreshUnassignedCard();
                    refreshProjectCard();
                    refreshSidebar();
                });
    }

    // ── 로그아웃 ─────────────────────────────────────────
    private void onLogout() {
        int r = JOptionPane.showConfirmDialog(this,
                "로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        dispose();
        new LoginView().setVisible(true);
    }

    // ════════════════════════════════════════════════════════
    //  셀 에디터 (버튼 클릭 처리)
    // ════════════════════════════════════════════════════════
    private class RemoveMemberEditor extends DefaultCellEditor {
        private int currentRow = -1;
        RemoveMemberEditor() {
            super(new JCheckBox());
            JButton btn = makeBtn("제거", new Color(0x7F1D1D));
            btn.addActionListener(e -> { fireEditingStopped(); doRemoveMember(currentRow); });
            editorComponent = btn;
        }
        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            currentRow = r; return editorComponent;
        }
        @Override public Object getCellEditorValue() { return "제거"; }
    }

    private class AssignUserEditor extends DefaultCellEditor {
        private int currentRow = -1;
        AssignUserEditor() {
            super(new JCheckBox());
            JButton btn = makeBtn("프로젝트 배정", new Color(0x1E40AF));
            btn.addActionListener(e -> { fireEditingStopped(); doAssignToProject(currentRow); });
            editorComponent = btn;
        }
        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            currentRow = r; return editorComponent;
        }
        @Override public Object getCellEditorValue() { return "프로젝트 배정"; }
    }

    // ════════════════════════════════════════════════════════
    //  사이드바 커스텀 렌더러
    // ════════════════════════════════════════════════════════
    private class SidebarRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            lbl.setBorder(new EmptyBorder(0, 14, 0, 8));
            lbl.setBackground(isSelected ? SEL_BG : BG_SIDEBAR);
            lbl.setForeground(FG_WHITE);

            String val = (String) value;
            if (UNASSIGNED_KEY.equals(val)) {
                lbl.setText("⚠  Unassigned");
                lbl.setForeground(C_UNASSIGN);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            } else {
                Project p = projects.stream()
                        .filter(pr -> pr.getName().equals(val))
                        .findFirst().orElse(null);
                String suffix = p != null ? "  (" + p.getMembers().size() + "명)" : "";
                lbl.setText("📁  " + val + suffix);
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            }
            return lbl;
        }
    }

    // ════════════════════════════════════════════════════════
    //  렌더러 헬퍼
    // ════════════════════════════════════════════════════════
    private TableCellRenderer roleBadgeRenderer() {
        return (t, v, s, f, r, c) -> {
            JLabel lbl = new JLabel(v != null ? v.toString() : "");
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(Color.WHITE);
            switch (v != null ? v.toString() : "") {
                case "ADMIN":     lbl.setBackground(C_ADMIN);   break;
                case "PL":        lbl.setBackground(C_PL);      break;
                case "DEVELOPER": lbl.setBackground(C_DEV);     break;
                case "TESTER":    lbl.setBackground(C_TESTER);  break;
                default:          lbl.setBackground(C_UNASSIGN);
            }
            lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
            return lbl;
        };
    }

    private TableCellRenderer statusBadgeRenderer() {
        return (t, v, s, f, r, c) -> {
            JLabel lbl = new JLabel(v != null ? v.toString() : "");
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(Color.WHITE);
            switch (v != null ? v.toString() : "") {
                case "ACTIVE":   lbl.setBackground(C_ACTIVE);   break;
                case "PENDING":  lbl.setBackground(C_PENDING);  break;
                case "REJECTED": lbl.setBackground(C_REJECTED); break;
                case "DISABLED": lbl.setBackground(C_DISABLED); break;
                default:         lbl.setBackground(BG_INPUT);
            }
            lbl.setBorder(new EmptyBorder(4, 8, 4, 8));
            return lbl;
        };
    }

    private TableCellRenderer btnRenderer(String label, Color bg) {
        return (t, v, s, f, r, c) -> makeBtn(label, bg);
    }

    // ════════════════════════════════════════════════════════
    //  공통 UI 헬퍼
    // ════════════════════════════════════════════════════════
    private JTable buildStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(BG_PANEL);
        table.setForeground(FG_WHITE);
        table.setGridColor(DIVIDER);
        table.setRowHeight(38);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setSelectionBackground(SEL_BG);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setBackground(BG_DARK);
        table.getTableHeader().setForeground(FG_LABEL);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer def = new DefaultTableCellRenderer();
        def.setBackground(BG_PANEL);
        def.setForeground(FG_WHITE);
        table.setDefaultRenderer(Object.class, def);
        return table;
    }

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(DIVIDER));
        sp.getViewport().setBackground(BG_PANEL);
    }

    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(FG_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));
        return btn;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(FG_LABEL);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    private JLabel makeFormLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(FG_LABEL);
        return l;
    }

    private void styleInputField(JTextField tf) {
        tf.setBackground(BG_INPUT);
        tf.setForeground(FG_WHITE);
        tf.setCaretColor(FG_WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER),
                new EmptyBorder(5, 8, 5, 8)));
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "경고", JOptionPane.WARNING_MESSAGE);
    }
}