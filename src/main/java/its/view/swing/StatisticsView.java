package its.view.swing;

import its.controller.CategoryController;
import its.controller.StatisticsController;
import its.model.Category;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.User;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class StatisticsView extends JDialog {

    private final StatisticsController statisticsController;
    private final CategoryController categoryController;
    private final Project project;
    private final User currentUser;
    private final int year;
    private final int month;
    private JTextField thresholdField;
    private JTextField categorySearchField;
    private DefaultTableModel clusteringModel;
    private JTable clusteringTable;
    private List<Category> previewCategories = new ArrayList<>();

    public StatisticsView(JFrame owner, StatisticsController statisticsController,
                          CategoryController categoryController, Project project, User currentUser) {
        super(owner, "Issue Statistics - " + project.getName(), false);
        this.statisticsController = statisticsController;
        this.categoryController = categoryController;
        this.project = project;
        this.currentUser = currentUser;
        LocalDate today = LocalDate.now();
        this.year = today.getYear();
        this.month = today.getMonthValue();
        initUI();
    }

    private void initUI() {
        setSize(760, 520);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel(project.getName() + " statistics (" + year + "-" + month + ")");
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Daily", buildDailyPanel());
        tabs.addTab("Monthly", buildMonthlyPanel());
        tabs.addTab("Summary", buildSummaryPanel());
        tabs.addTab("Classification", buildClusteringPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildDailyPanel() {
        Map<Integer, Long> dailyCounts = statisticsController.getIssueCountByDay(
                project.getProjectId(), year, month);
        return buildCountPanel(dailyCounts, "Day", "Issues");
    }

    private JPanel buildMonthlyPanel() {
        Map<Integer, Long> monthlyCounts = statisticsController.getIssueCountByMonth(
                project.getProjectId(), year);
        return buildCountPanel(monthlyCounts, "Month", "Issues");
    }

    private JPanel buildCountPanel(Map<Integer, Long> counts, String labelColumn, String countColumn) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new BarChartPanel(counts), BorderLayout.CENTER);
        panel.add(new JScrollPane(createCountTable(counts, labelColumn, countColumn)), BorderLayout.EAST);
        panel.add(new JLabel(getTrendText(counts)), BorderLayout.SOUTH);
        return panel;
    }

    private JTable createCountTable(Map<Integer, Long> counts, String labelColumn, String countColumn) {
        DefaultTableModel model = new DefaultTableModel(new String[]{labelColumn, countColumn}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        JTable table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(180, 360));
        return table;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        DefaultTableModel priorityModel = new DefaultTableModel(new String[]{"Priority", "Issues"}, 0);
        for (Map.Entry<Priority, Long> entry
                : statisticsController.getIssueCountByPriority(project.getProjectId()).entrySet()) {
            priorityModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        DefaultTableModel developerModel = new DefaultTableModel(new String[]{"Developer", "Resolved"}, 0);
        for (Map.Entry<User, Long> entry
                : statisticsController.getResolvedCountByDeveloper(project.getProjectId()).entrySet()) {
            String loginId = entry.getKey() != null ? entry.getKey().getLoginId() : "-";
            developerModel.addRow(new Object[]{loginId, entry.getValue()});
        }

        panel.add(new JScrollPane(new JTable(priorityModel)));
        panel.add(new JScrollPane(new JTable(developerModel)));
        return panel;
    }

    private JPanel buildClusteringPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        thresholdField = new JTextField("0.25", 6);
        JButton previewButton = new JButton("Preview");
        JButton saveButton = new JButton("Save");
        JButton searchButton = new JButton("Search");
        JButton renameButton = new JButton("Rename");
        JButton resetButton = new JButton("Reset");
        JButton mergeButton = new JButton("Merge");
        JButton splitButton = new JButton("Split");
        categorySearchField = new JTextField(14);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftControls.add(new JLabel("Cosine threshold"));
        leftControls.add(thresholdField);
        leftControls.add(previewButton);
        leftControls.add(saveButton);
        leftControls.add(renameButton);
        leftControls.add(resetButton);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightControls.add(categorySearchField);
        rightControls.add(searchButton);

        JPanel controlPanel = new JPanel(new BorderLayout(8, 0));
        controlPanel.add(leftControls, BorderLayout.WEST);
        controlPanel.add(rightControls, BorderLayout.EAST);

        clusteringModel = new DefaultTableModel(
                new String[]{"ID", "Name", "Keywords", "Issue Count", "Issues"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        clusteringTable = new JTable(clusteringModel);
        clusteringTable.setRowHeight(28);
        clusteringTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        clusteringTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        clusteringTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        clusteringTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        clusteringTable.getColumnModel().getColumn(4).setPreferredWidth(360);
        clusteringTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        previewButton.addActionListener(e -> previewClustering());
        saveButton.addActionListener(e -> saveClustering());
        searchButton.addActionListener(e -> searchCategories());
        renameButton.addActionListener(e -> renameSelectedCategory());
        resetButton.addActionListener(e -> resetCategories());
        mergeButton.addActionListener(e -> mergeSelectedCategories());
        splitButton.addActionListener(e -> splitSelectedCategory());
        categorySearchField.addActionListener(e -> searchCategories());

        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.add(new JScrollPane(clusteringTable), BorderLayout.CENTER);

        JPanel categoryEditPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        categoryEditPanel.add(mergeButton);
        categoryEditPanel.add(splitButton);
        tablePanel.add(categoryEditPanel, BorderLayout.SOUTH);

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        panel.add(new JLabel("Preview checks TF-IDF/cosine clustering. Save stores current categories; Search filters current categories."),
                BorderLayout.SOUTH);
        return panel;
    }

    private void previewClustering() {
        try {
            double threshold = readThreshold();
            previewCategories = categoryController.previewCategories(project, threshold, currentUser);
            renderCurrentCategories();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveClustering() {
        try {
            double threshold = readThreshold();
            if (previewCategories == null || previewCategories.isEmpty()) {
                previewCategories = categoryController.previewCategories(project, threshold, currentUser);
                renderCurrentCategories();
            }

            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Save this classification to the project?",
                    "Save",
                    JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }

            List<Category> savedCategories = categoryController.saveCategories(project, previewCategories, currentUser);
            previewCategories = savedCategories;
            renderCurrentCategories();
            JOptionPane.showMessageDialog(this, "Classification saved.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchCategories() {
        try {
            if (previewCategories == null || previewCategories.isEmpty()) {
                previewCategories = categoryController.findCategories(project, currentUser);
            }

            List<Category> visibleCategories = getVisibleCategories();
            renderCategories(visibleCategories);
            if (visibleCategories.isEmpty()) {
                boolean hasKeyword = !categorySearchField.getText().trim().isEmpty();
                JOptionPane.showMessageDialog(this,
                        hasKeyword ? "No matching categories." : "No saved categories.");
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean matchesCategory(Category category, String keyword) {
        if (category == null) {
            return false;
        }

        if (String.valueOf(category.getCategoryId()).contains(keyword)) {
            return true;
        }

        if (category.getCategoryName() != null
                && category.getCategoryName().toLowerCase().contains(keyword)) {
            return true;
        }

        if (getTopKeywords(category, 10).toLowerCase().contains(keyword)) {
            return true;
        }

        return category.getIssues().stream()
                .anyMatch(issue -> issue != null
                        && (String.valueOf(issue.getIssueId()).contains(keyword)
                        || (issue.getTitle() != null && issue.getTitle().toLowerCase().contains(keyword))));
    }

    private List<Category> ensurePreviewCategories() {
        if (previewCategories == null || previewCategories.isEmpty()) {
            previewCategories = categoryController.findCategories(project, currentUser);
        }

        return previewCategories;
    }

    private List<Category> getVisibleCategories() {
        List<Category> categories = ensurePreviewCategories();
        String keyword = categorySearchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            return categories;
        }

        return categories.stream()
                .filter(category -> matchesCategory(category, keyword))
                .collect(Collectors.toList());
    }

    private void renderCurrentCategories() {
        renderCategories(getVisibleCategories());
    }

    private void renameCategoryInPreview(int categoryId, String newName) {
        String trimmedName = newName.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Invalid category name.");
        }

        Category targetCategory = null;
        for (Category category : ensurePreviewCategories()) {
            if (category == null) {
                continue;
            }

            if (category.getCategoryId() == categoryId) {
                targetCategory = category;
            }
            else if (trimmedName.equalsIgnoreCase(category.getCategoryName())) {
                throw new IllegalArgumentException("Category name already exists.");
            }
        }

        if (targetCategory == null) {
            throw new IllegalArgumentException("Target category does not exist.");
        }

        targetCategory.setCategoryName(trimmedName);
    }

    private void renameSelectedCategory() {
        int selectedRow = clusteringTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a category first.");
            return;
        }

        int modelRow = clusteringTable.convertRowIndexToModel(selectedRow);
        int categoryId = ((Number) clusteringModel.getValueAt(modelRow, 0)).intValue();
        String currentName = String.valueOf(clusteringModel.getValueAt(modelRow, 1));

        String newName = JOptionPane.showInputDialog(this, "New category name:", currentName);
        if (newName == null) {
            return;
        }

        try {
            renameCategoryInPreview(categoryId, newName);
            renderCurrentCategories();
            JOptionPane.showMessageDialog(this, "Category name updated.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mergeSelectedCategories() {
        int[] selectedRows = clusteringTable.getSelectedRows();
        if (selectedRows.length < 2) {
            JOptionPane.showMessageDialog(this, "Select two or more categories.");
            return;
        }

        List<Integer> categoryIds = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = clusteringTable.convertRowIndexToModel(selectedRow);
            categoryIds.add(((Number) clusteringModel.getValueAt(modelRow, 0)).intValue());
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Merge selected categories into category #" + categoryIds.get(0) + "?",
                "Merge Categories",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            List<Category> categories = ensurePreviewCategories();
            int targetCategoryId = categoryIds.get(0);
            for (int i = 1; i < categoryIds.size(); i++) {
                categories = categoryController.previewMergeCategories(
                        project, categories, targetCategoryId, categoryIds.get(i), currentUser);
            }

            previewCategories = categories;
            renderCurrentCategories();
            JOptionPane.showMessageDialog(this, "Categories merged.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void splitSelectedCategory() {
        int selectedRow = clusteringTable.getSelectedRow();
        if (selectedRow < 0 || clusteringTable.getSelectedRows().length != 1) {
            JOptionPane.showMessageDialog(this, "Select one category to split.");
            return;
        }

        int modelRow = clusteringTable.convertRowIndexToModel(selectedRow);
        int categoryId = ((Number) clusteringModel.getValueAt(modelRow, 0)).intValue();

        try {
            List<Category> categories = ensurePreviewCategories();
            Category targetCategory = findCategoryById(categories, categoryId);
            if (targetCategory == null || targetCategory.getIssues().size() < 2) {
                JOptionPane.showMessageDialog(this, "A category needs at least two issues to split.");
                return;
            }

            List<Long> issueIds = chooseIssuesToSplit(targetCategory);
            if (issueIds.isEmpty()) {
                return;
            }

            categories = categoryController.previewPartitionCategory(
                    project, categories, categoryId, issueIds, currentUser);
            previewCategories = categories;
            renderCurrentCategories();
            JOptionPane.showMessageDialog(this, "Category split.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Long> chooseIssuesToSplit(Category category) {
        DefaultListModel<IssueOption> listModel = new DefaultListModel<>();
        for (Issue issue : category.getIssues()) {
            if (issue != null) {
                listModel.addElement(new IssueOption(issue));
            }
        }

        JList<IssueOption> issueList = new JList<>(listModel);
        issueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        issueList.setVisibleRowCount(Math.min(10, Math.max(4, listModel.size())));

        int result = JOptionPane.showConfirmDialog(
                this,
                new JScrollPane(issueList),
                "Select Issues To Split",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return new ArrayList<>();
        }

        List<IssueOption> selectedIssues = issueList.getSelectedValuesList();
        if (selectedIssues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one issue.");
            return new ArrayList<>();
        }
        if (selectedIssues.size() >= category.getIssues().size()) {
            JOptionPane.showMessageDialog(this, "Original category cannot be empty.");
            return new ArrayList<>();
        }

        return selectedIssues.stream()
                .map(IssueOption::getIssueId)
                .collect(Collectors.toList());
    }

    private Category findCategoryById(List<Category> categories, int categoryId) {
        if (categories == null) {
            return null;
        }

        for (Category category : categories) {
            if (category != null && category.getCategoryId() == categoryId) {
                return category;
            }
        }

        return null;
    }

    private void resetCategories() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Reset all categories for this project?",
                "Reset Categories",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            categoryController.resetCategories(project, currentUser);
            previewCategories = new ArrayList<>();
            renderCategories(previewCategories);
            JOptionPane.showMessageDialog(this, "Categories reset.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Classification Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double readThreshold() {
        try {
            double threshold = Double.parseDouble(thresholdField.getText().trim());
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException();
            }
            return threshold;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Threshold must be a number between 0.0 and 1.0.");
        }
    }

    private void renderCategories(List<Category> categories) {
        clusteringModel.setRowCount(0);
        if (categories == null) {
            return;
        }

        for (Category category : categories) {
            String issues = category.getIssues().stream()
                    .map(issue -> "#" + issue.getIssueId() + " " + issue.getTitle())
                    .collect(Collectors.joining(", "));
            clusteringModel.addRow(new Object[]{
                    category.getCategoryId(),
                    category.getCategoryName(),
                    getTopKeywords(category, 5),
                    category.getIssues().size(),
                    issues
            });
        }
    }

    private String getTopKeywords(Category category, int limit) {
        if (category == null || category.getRepresentVector() == null) {
            return "";
        }

        return category.getRepresentVector().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0.0)
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }

    private String getTrendText(Map<Integer, Long> counts) {
        long firstHalf = 0;
        long secondHalf = 0;
        int half = counts.size() / 2;
        int index = 0;

        for (Long count : counts.values()) {
            if (index++ < half) firstHalf += count;
            else secondHalf += count;
        }

        if (secondHalf > firstHalf) return "Trend: increasing";
        if (secondHalf < firstHalf) return "Trend: decreasing";
        return "Trend: stable";
    }

    private static class BarChartPanel extends JPanel {
        private final Map<Integer, Long> counts;

        private BarChartPanel(Map<Integer, Long> counts) {
            this.counts = counts;
            setPreferredSize(new Dimension(460, 340));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();
            int left = 42;
            int bottom = 34;
            int top = 16;
            int chartWidth = Math.max(1, width - left - 16);
            int chartHeight = Math.max(1, height - top - bottom);
            long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);

            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(left, top, left, top + chartHeight);
            g2.drawLine(left, top + chartHeight, left + chartWidth, top + chartHeight);

            int size = Math.max(1, counts.size());
            int barWidth = Math.max(4, chartWidth / size - 3);
            int i = 0;
            for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
                int x = left + i * chartWidth / size + 2;
                int barHeight = max == 0 ? 0 : (int) ((entry.getValue() * chartHeight) / max);
                int y = top + chartHeight - barHeight;
                g2.setColor(new Color(62, 122, 176));
                g2.fillRect(x, y, barWidth, barHeight);
                if (size <= 12 || entry.getKey() % 5 == 0 || entry.getKey() == 1) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(String.valueOf(entry.getKey()), x, top + chartHeight + 16);
                }
                i++;
            }
        }
    }

    private static class IssueOption {
        private final Issue issue;

        private IssueOption(Issue issue) {
            this.issue = issue;
        }

        private long getIssueId() {
            return issue.getIssueId();
        }

        @Override
        public String toString() {
            return "#" + issue.getIssueId() + " " + issue.getTitle();
        }
    }
}
