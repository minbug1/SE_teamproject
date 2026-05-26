package its.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import its.controller.IssueController;
import its.controller.ProjectController;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.User;



public class ReportIssueView extends JDialog {

    private IssueController issueController;
    private ProjectController projectController;
    private User currentUser;
    private MainView mainView;
    private final Project currentProject;

    // 기본 정보 탭 컴포넌트
    private JTextField titleField;
    private JTextArea descArea;
    private JComboBox<Priority> priorityBox;

    // 코멘트 탭 컴포넌트
    private JTextArea commentArea;

    private JButton submitBtn;
    private JButton cancelBtn;

    public ReportIssueView(MainView mainView,
                            IssueController issueController,
                            ProjectController projectController,
                            User currentUser,
                            Project currentProject) {
        super(mainView, "새 이슈 등록", true);
        this.mainView = mainView;
        this.issueController = issueController;
        this.projectController = projectController;
        this.currentUser = currentUser;
        this.currentProject = currentProject;
        initUI();
    }

    private void initUI() {
        setSize(500, 450);
        setLocationRelativeTo(mainView);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // JTabbedPane으로 탭 구성
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("기본 정보", createBasicInfoPanel());
        tabbedPane.addTab("코멘트", createCommentPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // ── 기본 정보 탭 ───────────────────────────
    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 제목
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("제목 *"), gbc);
        titleField = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(titleField, gbc);

        // 내용
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("내용"), gbc);
        descArea = new JTextArea(5, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(descArea), gbc);

        // 우선순위
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0; gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("우선순위 *"), gbc);
        priorityBox = new JComboBox<>(Priority.values());
        priorityBox.setSelectedItem(Priority.MAJOR);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(priorityBox, gbc);

        // Reporter (읽기 전용)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Reporter"), gbc);
        JTextField reporterField = 
            new JTextField(String.valueOf(currentUser.getUserId()));
        reporterField.setEditable(false);
        reporterField.setBackground(Color.LIGHT_GRAY);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(reporterField, gbc);

        return panel;
    }

    // ── 코멘트 탭 ──────────────────────────────
    private JPanel createCommentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        JLabel guide = new JLabel(
            "이슈 등록 시 추가할 코멘트를 입력하세요. (선택)"
        );
        guide.setForeground(Color.GRAY);
        guide.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        commentArea = new JTextArea();
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        panel.add(guide, BorderLayout.NORTH);
        panel.add(new JScrollPane(commentArea), BorderLayout.CENTER);
        return panel;
    }

    // ── 버튼 패널 ──────────────────────────────
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));

        cancelBtn = new JButton("취소");
        submitBtn = new JButton("등록");
        submitBtn.setBackground(new Color(70, 130, 180));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setOpaque(true);

        cancelBtn.addActionListener(e -> onCancel());
        submitBtn.addActionListener(e -> onSubmit());

        getRootPane().setDefaultButton(submitBtn);
        getRootPane().registerKeyboardAction(
            e -> onCancel(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        panel.add(cancelBtn);
        panel.add(submitBtn);
        return panel;
    }

    // 이벤트 핸들러 
    private void onSubmit() {
        String title    = titleField.getText().trim();
        String desc     = descArea.getText().trim();
        Priority priority = (Priority) priorityBox.getSelectedItem();
        String comment  = commentArea.getText().trim();
                                         // 코멘트는 빈 문자열도 허용

        // 제목 필수 검증
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "제목을 입력해주세요.",
                "입력 오류", JOptionPane.WARNING_MESSAGE
            );
            titleField.requestFocus();
            return;
        }

        try {
            Issue created = issueController.reportIssue(
                currentProject, title, desc, currentUser, priority, comment
            );
            projectController.addIssueToProject(currentProject, created);

            JOptionPane.showMessageDialog(
                this,
                "이슈 #" + created.getIssueId() + " 가 등록되었습니다.",
                "등록 완료", JOptionPane.INFORMATION_MESSAGE
            );

            mainView.refreshTable();
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this, "이슈 등록 실패: " + ex.getMessage(),
                "오류", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onCancel() {
        // 입력값이 있을 때만 확인 팝업
        boolean hasInput = !titleField.getText().trim().isEmpty()
                        || !descArea.getText().trim().isEmpty()
                        || !commentArea.getText().trim().isEmpty();

        if (hasInput) {
            int confirm = JOptionPane.showConfirmDialog(
                this, "작성 중인 내용이 사라집니다. 취소하시겠습니까?",
                "취소 확인", JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        dispose();
    }
}
