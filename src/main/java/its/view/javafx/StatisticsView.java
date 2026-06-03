package its.view.javafx;

import its.controller.CategoryController;
import its.controller.StatisticsController;
import its.model.Category;
import its.model.Issue;
import its.model.Priority;
import its.model.Project;
import its.model.User;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatisticsView {

    private final Stage owner;
    private final StatisticsController statisticsController;
    private final CategoryController categoryController;
    private final Project project;
    private final User currentUser;
    private final int year;
    private final int month;

    // Clustering tab state
    private TextField thresholdField;
    private TextField searchField;
    private TableView<ClusterRow> clusteringTable;
    private List<Category> previewCategories = new ArrayList<>();

    public StatisticsView(Stage owner,
                          StatisticsController statisticsController,
                          CategoryController categoryController,
                          Project project,
                          User currentUser) {
        this.owner = owner;
        this.statisticsController = statisticsController;
        this.categoryController = categoryController;
        this.project = project;
        this.currentUser = currentUser;
        LocalDate today = LocalDate.now();
        this.year = today.getYear();
        this.month = today.getMonthValue();
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.setTitle("Issue Statistics - " + project.getName());
        dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);
        dialog.setScene(buildScene());
        dialog.setWidth(820);
        dialog.setHeight(580);
        dialog.show();
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Label header = new Label(
                project.getName() + " statistics (" + year + "-" + month + ")");
        BorderPane.setMargin(header, new Insets(0, 0, 8, 0));
        root.setTop(header);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(buildTab("Daily",
                buildCountPane(statisticsController.getIssueCountByDay(
                        project.getProjectId(), year, month), "Day")));
        tabs.getTabs().add(buildTab("Monthly",
                buildCountPane(statisticsController.getIssueCountByMonth(
                        project.getProjectId(), year), "Month")));
        tabs.getTabs().add(buildTab("Summary", buildSummaryPane()));
        tabs.getTabs().add(buildTab("Classification", buildClusteringPane()));

        root.setCenter(tabs);
        return new Scene(root);
    }

    private Tab buildTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    // ── Daily / Monthly ───────────────────────────────────────────────────────

    private BorderPane buildCountPane(Map<Integer, Long> counts, String label) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(2);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((k, v) -> series.getData().add(
                new XYChart.Data<>(String.valueOf(k), v)));
        chart.getData().add(series);

        TableView<CountRow> table = buildCountTable(counts, label);
        table.setPrefWidth(200);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));
        pane.setCenter(chart);
        pane.setRight(table);
        pane.setBottom(new Label(getTrendText(counts)));
        return pane;
    }

    private TableView<CountRow> buildCountTable(Map<Integer, Long> counts, String labelCol) {
        TableView<CountRow> table = new TableView<>();
        TableColumn<CountRow, String> col1 = new TableColumn<>(labelCol);
        col1.setCellValueFactory(new PropertyValueFactory<>("label"));
        TableColumn<CountRow, Long> col2 = new TableColumn<>("Issues");
        col2.setCellValueFactory(new PropertyValueFactory<>("count"));
        table.getColumns().add(col1);
        table.getColumns().add(col2);
        counts.forEach((k, v) -> table.getItems().add(new CountRow(String.valueOf(k), v)));
        return table;
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    private HBox buildSummaryPane() {
        TableView<CountRow> priorityTable = new TableView<>();
        addSummaryColumns(priorityTable, "Priority", "Issues");
        statisticsController.getIssueCountByPriority(project.getProjectId())
                .forEach((p, c) -> priorityTable.getItems()
                        .add(new CountRow(p.name(), c)));

        TableView<CountRow> devTable = new TableView<>();
        addSummaryColumns(devTable, "Developer", "Resolved");
        statisticsController.getResolvedCountByDeveloper(project.getProjectId())
                .forEach((u, c) -> devTable.getItems()
                        .add(new CountRow(u != null ? u.getLoginId() : "-", c)));

        HBox pane = new HBox(8, priorityTable, devTable);
        pane.setPadding(new Insets(8));
        HBox.setHgrow(priorityTable, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(devTable, javafx.scene.layout.Priority.ALWAYS);
        return pane;
    }

    private void addSummaryColumns(TableView<CountRow> table, String label, String count) {
        TableColumn<CountRow, String> c1 = new TableColumn<>(label);
        c1.setCellValueFactory(new PropertyValueFactory<>("label"));
        TableColumn<CountRow, Long> c2 = new TableColumn<>(count);
        c2.setCellValueFactory(new PropertyValueFactory<>("count"));
        table.getColumns().add(c1);
        table.getColumns().add(c2);
    }

    // ── Clustering tab ────────────────────────────────────────────────────────

    private BorderPane buildClusteringPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));

        // Controls — top row
        thresholdField = new TextField("0.25");
        thresholdField.setPrefWidth(70);
        searchField = new TextField();
        searchField.setPromptText("Search categories…");
        searchField.setPrefWidth(180);

        Button previewBtn  = new Button("Preview");
        Button saveBtn     = new Button("Save");
        Button renameBtn   = new Button("Rename");
        Button mergeBtn    = new Button("Merge");
        Button splitBtn    = new Button("Split");
        Button resetBtn    = new Button("Reset");
        Button searchBtn   = new Button("Search");

        previewBtn.setOnAction(e -> onPreview());
        saveBtn   .setOnAction(e -> onSave());
        renameBtn .setOnAction(e -> onRename());
        mergeBtn  .setOnAction(e -> onMerge());
        splitBtn  .setOnAction(e -> onSplit());
        resetBtn  .setOnAction(e -> onReset());
        searchBtn .setOnAction(e -> onSearch());
        searchField.setOnAction(e -> onSearch());

        HBox leftBar = new HBox(6,
                new Label("Cosine threshold"), thresholdField,
                previewBtn, saveBtn, renameBtn, mergeBtn, splitBtn, resetBtn);
        leftBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox rightBar = new HBox(6, searchField, searchBtn);
        rightBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        HBox.setHgrow(rightBar, javafx.scene.layout.Priority.ALWAYS);

        HBox controls = new HBox(leftBar, rightBar);
        controls.setPadding(new Insets(0, 0, 8, 0));

        // Table
        clusteringTable = buildClusteringTable();

        Label hint = new Label(
                "Preview runs TF-IDF/cosine clustering. "
                + "Select rows then Merge/Split. Save stores the current classification.");

        pane.setTop(controls);
        pane.setCenter(clusteringTable);
        pane.setBottom(hint);
        return pane;
    }

    private TableView<ClusterRow> buildClusteringTable() {
        TableView<ClusterRow> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<ClusterRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        idCol.setPrefWidth(50);

        TableColumn<ClusterRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);

        TableColumn<ClusterRow, String> kwCol = new TableColumn<>("Keywords");
        kwCol.setCellValueFactory(new PropertyValueFactory<>("keywords"));
        kwCol.setPrefWidth(200);

        TableColumn<ClusterRow, Integer> cntCol = new TableColumn<>("Count");
        cntCol.setCellValueFactory(new PropertyValueFactory<>("issueCount"));
        cntCol.setPrefWidth(60);

        TableColumn<ClusterRow, String> issueCol = new TableColumn<>("Issues");
        issueCol.setCellValueFactory(new PropertyValueFactory<>("issues"));
        issueCol.setPrefWidth(360);

        table.getColumns().addAll(idCol, nameCol, kwCol, cntCol, issueCol);
        return table;
    }

    // ── Clustering actions ────────────────────────────────────────────────────

    private void onPreview() {
        try {
            double threshold = readThreshold();
            previewCategories = categoryController.previewCategories(
                    project, threshold, currentUser);
            renderCategories(getVisibleCategories());
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void onSave() {
        try {
            double threshold = readThreshold();
            if (previewCategories == null || previewCategories.isEmpty()) {
                previewCategories = categoryController.previewCategories(
                        project, threshold, currentUser);
                renderCategories(getVisibleCategories());
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Save this classification to the project?",
                    ButtonType.YES, ButtonType.NO);
            confirm.initOwner(owner);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.YES) return;
                List<Category> saved = categoryController.saveCategories(
                        project, previewCategories, currentUser);
                previewCategories = saved;
                renderCategories(getVisibleCategories());
                showInfo("Classification saved.");
            });
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void onSearch() {
        try {
            ensurePreviewCategories();
            renderCategories(getVisibleCategories());
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    /** Rename the single selected category (in previewCategories only). */
    private void onRename() {
        List<ClusterRow> selected = clusteringTable.getSelectionModel()
                .getSelectedItems();
        if (selected.size() != 1) {
            showWarning("Select exactly one category to rename.");
            return;
        }
        int categoryId = selected.get(0).getCategoryId();
        String currentName = selected.get(0).getName();

        TextInputDialog dialog = new TextInputDialog(currentName);
        dialog.setTitle("Rename Category");
        dialog.setHeaderText(null);
        dialog.setContentText("New category name:");
        dialog.initOwner(owner);
        dialog.showAndWait().ifPresent(newName -> {
            try {
                renameCategoryInPreview(categoryId, newName);
                renderCategories(getVisibleCategories());
                showInfo("Category renamed.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /** Merge all selected categories into the first one. */
    private void onMerge() {
        List<ClusterRow> selected = new ArrayList<>(
                clusteringTable.getSelectionModel().getSelectedItems());
        if (selected.size() < 2) {
            showWarning("Select two or more categories to merge.");
            return;
        }

        List<Integer> ids = selected.stream()
                .map(ClusterRow::getCategoryId)
                .collect(Collectors.toList());
        int targetId = ids.get(0);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Merge selected categories into category #" + targetId + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.initOwner(owner);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            try {
                List<Category> categories = ensurePreviewCategories();
                for (int i = 1; i < ids.size(); i++) {
                    categories = categoryController.previewMergeCategories(
                            project, categories, targetId, ids.get(i), currentUser);
                }
                previewCategories = categories;
                renderCategories(getVisibleCategories());
                showInfo("Categories merged.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /** Split selected issues out of the selected category into a new one. */
    private void onSplit() {
        List<ClusterRow> selected = clusteringTable.getSelectionModel()
                .getSelectedItems();
        if (selected.size() != 1) {
            showWarning("Select exactly one category to split.");
            return;
        }

        int categoryId = selected.get(0).getCategoryId();
        List<Category> categories = ensurePreviewCategories();
        Category target = findCategoryById(categories, categoryId);

        if (target == null || target.getIssues().size() < 2) {
            showWarning("A category needs at least two issues to split.");
            return;
        }

        List<Long> chosenIds = chooseIssuesToSplit(target);
        if (chosenIds.isEmpty()) return;

        try {
            categories = categoryController.previewPartitionCategory(
                    project, categories, categoryId, chosenIds, currentUser);
            previewCategories = categories;
            renderCategories(getVisibleCategories());
            showInfo("Category split.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void onReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Reset all categories for this project?",
                ButtonType.YES, ButtonType.NO);
        confirm.initOwner(owner);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            try {
                categoryController.resetCategories(project, currentUser);
                previewCategories = new ArrayList<>();
                renderCategories(previewCategories);
                showInfo("Categories reset.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Show a dialog listing the issues in {@code target}; the user picks which
     * ones to move into the new split category.
     *
     * @return list of issue IDs to split out, empty if cancelled or invalid
     */
    private List<Long> chooseIssuesToSplit(Category target) {
        ListView<IssueOption> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        for (Issue issue : target.getIssues()) {
            if (issue != null) {
                listView.getItems().add(new IssueOption(issue));
            }
        }
        listView.setPrefHeight(Math.min(10, Math.max(4, listView.getItems().size())) * 28.0);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Select Issues To Split");
        dialog.setHeaderText(
                "Select the issues to move into a new category.\n"
                + "At least one issue must remain in the original category.");
        dialog.getDialogPane().setContent(listView);
        dialog.initOwner(owner);
        dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return new ArrayList<>();
        }

        List<IssueOption> chosen = listView.getSelectionModel().getSelectedItems();
        if (chosen.isEmpty()) {
            showWarning("Select at least one issue.");
            return new ArrayList<>();
        }
        if (chosen.size() >= target.getIssues().size()) {
            showWarning("Original category cannot be left empty.");
            return new ArrayList<>();
        }

        return chosen.stream()
                .map(IssueOption::getIssueId)
                .collect(Collectors.toList());
    }

    private List<Category> ensurePreviewCategories() {
        if (previewCategories == null || previewCategories.isEmpty()) {
            previewCategories = categoryController.findCategories(project, currentUser);
        }
        return previewCategories;
    }

    private List<Category> getVisibleCategories() {
        List<Category> all = ensurePreviewCategories();
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) return all;
        return all.stream()
                .filter(c -> matchesCategory(c, keyword))
                .collect(Collectors.toList());
    }

    private boolean matchesCategory(Category category, String keyword) {
        if (category == null) return false;
        if (String.valueOf(category.getCategoryId()).contains(keyword)) return true;
        if (category.getCategoryName() != null
                && category.getCategoryName().toLowerCase().contains(keyword)) return true;
        if (getTopKeywords(category, 10).toLowerCase().contains(keyword)) return true;
        return category.getIssues().stream().anyMatch(issue ->
                issue != null && (
                        String.valueOf(issue.getIssueId()).contains(keyword)
                        || (issue.getTitle() != null
                                && issue.getTitle().toLowerCase().contains(keyword))));
    }

    private void renameCategoryInPreview(int categoryId, String newName) {
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Category name must not be empty.");
        }

        Category target = null;
        for (Category c : ensurePreviewCategories()) {
            if (c == null) continue;
            if (c.getCategoryId() == categoryId) {
                target = c;
            } else if (trimmed.equalsIgnoreCase(c.getCategoryName())) {
                throw new IllegalArgumentException("Category name already exists.");
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Target category does not exist.");
        }
        target.setCategoryName(trimmed);
    }

    private Category findCategoryById(List<Category> categories, int categoryId) {
        if (categories == null) return null;
        for (Category c : categories) {
            if (c != null && c.getCategoryId() == categoryId) return c;
        }
        return null;
    }

    private void renderCategories(List<Category> categories) {
        clusteringTable.getItems().clear();
        if (categories == null) return;
        for (Category c : categories) {
            String issues = c.getIssues().stream()
                    .map(i -> "#" + i.getIssueId() + " " + i.getTitle())
                    .collect(Collectors.joining(", "));
            clusteringTable.getItems().add(new ClusterRow(
                    c.getCategoryId(),
                    c.getCategoryName(),
                    getTopKeywords(c, 5),
                    c.getIssues().size(),
                    issues));
        }
    }

    private String getTopKeywords(Category category, int limit) {
        if (category == null || category.getRepresentVector() == null) return "";
        return category.getRepresentVector().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0.0)
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }

    private double readThreshold() {
        try {
            double v = Double.parseDouble(thresholdField.getText().trim());
            if (v < 0.0 || v > 1.0) throw new NumberFormatException();
            return v;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Threshold must be a number between 0.0 and 1.0.");
        }
    }

    private String getTrendText(Map<Integer, Long> counts) {
        long first = 0, second = 0;
        int half = counts.size() / 2, i = 0;
        for (Long v : counts.values()) {
            if (i++ < half) first += v; else second += v;
        }
        if (second > first) return "Trend: increasing";
        if (second < first) return "Trend: decreasing";
        return "Trend: stable";
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.initOwner(owner);
        a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.initOwner(owner);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(owner);
        a.showAndWait();
    }

    // ── Row model classes ─────────────────────────────────────────────────────

    public static class CountRow {
        private final String label;
        private final Long count;

        public CountRow(String label, Long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() { return label; }
        public Long getCount()   { return count; }
    }

    public static class ClusterRow {
        private final Integer categoryId;
        private final String  name;
        private final String  keywords;
        private final Integer issueCount;
        private final String  issues;

        public ClusterRow(Integer categoryId, String name, String keywords,
                          Integer issueCount, String issues) {
            this.categoryId = categoryId;
            this.name       = name;
            this.keywords   = keywords;
            this.issueCount = issueCount;
            this.issues     = issues;
        }

        public Integer getCategoryId() { return categoryId; }
        public String  getName()       { return name; }
        public String  getKeywords()   { return keywords; }
        public Integer getIssueCount() { return issueCount; }
        public String  getIssues()     { return issues; }
    }

    private static class IssueOption {
        private final Issue issue;

        IssueOption(Issue issue) { this.issue = issue; }

        long getIssueId() { return issue.getIssueId(); }

        @Override
        public String toString() {
            return "#" + issue.getIssueId() + " " + issue.getTitle();
        }
    }
}