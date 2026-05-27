package its.view.swing;

import java.awt.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import its.model.Comment;
import its.model.Issue;

public class IssueDetailView extends JDialog {

    public IssueDetailView(Component parent, Issue issue) {
        super(SwingUtilities.getWindowAncestor(parent), "Issue Detail", ModalityType.APPLICATION_MODAL);
        initUI(issue);
    }

    private void initUI(Issue issue) {
        setSize(560, 480);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        // ── 상단: 이슈 기본 정보 ──
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 4, 4));
        infoPanel.setBorder(BorderFactory.createTitledBorder("기본 정보"));
        addInfo(infoPanel, "ID",        String.valueOf(issue.getIssueId()));
        addInfo(infoPanel, "Title",     issue.getTitle());
        addInfo(infoPanel, "Priority",  String.valueOf(issue.getPriority()));
        addInfo(infoPanel, "Status",    String.valueOf(issue.getStatus()));
        addInfo(infoPanel, "Reporter",  loginIdOrDash(issue.getReporter()));
        addInfo(infoPanel, "Assignee",  loginIdOrDash(issue.getAssignee()));
        addInfo(infoPanel, "Fixer",     loginIdOrDash(issue.getFixer()));

        // ── 중단: Description ──
        JTextArea descArea = new JTextArea(issue.getDescription());
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createTitledBorder("Description"));
        descScroll.setPreferredSize(new Dimension(0, 80));

        // ── 하단: 코멘트 목록 테이블 ──
        String[] columns = {"#", "Author", "Date", "Content"};
        DefaultTableModel commentModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Comment c : issue.getComments()) {
            commentModel.addRow(new Object[]{
                c.getCommentId(),
                c.getAuthor() != null ? c.getAuthor().getLoginId() : "-",
                c.getWrittenDate() != null ? c.getWrittenDate().toString() : "-",
                c.getContent()
            });
        }

        JTable commentTable = new JTable(commentModel);
        commentTable.setRowHeight(28);
        commentTable.getTableHeader().setReorderingAllowed(false);
        commentTable.getColumnModel().getColumn(0).setMaxWidth(36);   // #
        commentTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Author
        commentTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Date
        commentTable.getColumnModel().getColumn(3).setPreferredWidth(280); // Content

        JScrollPane commentScroll = new JScrollPane(commentTable);
        commentScroll.setBorder(BorderFactory.createTitledBorder(
                "Comments (" + issue.getComments().size() + ")"));

        // ── 닫기 버튼 ──
        JButton closeBtn = new JButton("닫기");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);

        // ── 레이아웃 조립 ──
        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.add(descScroll, BorderLayout.NORTH);
        centerPanel.add(commentScroll, BorderLayout.CENTER);

        add(infoPanel,   BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(btnPanel,    BorderLayout.SOUTH);
    }

    private void addInfo(JPanel panel, String label, String value) {
        panel.add(new JLabel(label + " :"));
        panel.add(new JLabel(value));
    }

    private String loginIdOrDash(its.model.User user) {
        return user == null ? "-" : user.getLoginId();
    }
}
