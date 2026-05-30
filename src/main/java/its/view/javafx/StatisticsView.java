package its.view.javafx;

import its.controller.CategoryController;
import its.controller.StatisticsController;
import its.model.Category;
import its.model.Priority;
import its.model.Project;
import its.model.User;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsView {

    private final Stage owner;
    private final StatisticsController statisticsController;
    private final CategoryController categoryController;
    private final Project project;
    private final User currentUser;
    private final int year;
    private final int month;
    private TextField thresholdField;
    private TableView<ClusterRow> clusteringTable;
    private List<Category> previewCategories = new ArrayList<>();

    public StatisticsView(Stage owner, StatisticsController statisticsController,
                          CategoryController categoryController, Project project, User currentUser) {
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
        dialog.setWidth(760);
        dialog.setHeight(520);
        dialog.show();
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Label header = new Label(project.getName() + " statistics (" + year + "-" + month + ")");
        root.setTop(header);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Daily", buildCountPane(
                statisticsController.getIssueCountByDay(project.getProjectId(), year, month), "Day")));
        tabs.getTabs().add(new Tab("Monthly", buildCountPane(
                statisticsController.getIssueCountByMonth(project.getProjectId(), year), "Month")));
        tabs.getTabs().add(new Tab("Summary", buildSummaryPane()));
        tabs.getTabs().add(new Tab("Clustering", buildClusteringPane()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));

        root.setCenter(tabs);
        return new Scene(root);
    }

    private BorderPane buildCountPane(Map<Integer, Long> counts, String label) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));

        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(2);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((key, value) -> series.getData().add(
                new XYChart.Data<>(String.valueOf(key), value)));
        chart.getData().add(series);

        TableView<CountRow> table = new TableView<>();
        table.setPrefWidth(180);
        TableColumn<CountRow, String> labelColumn = new TableColumn<>(label);
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        TableColumn<CountRow, Long> countColumn = new TableColumn<>("Issues");
        countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        table.getColumns().add(labelColumn);
        table.getColumns().add(countColumn);
        counts.forEach((key, value) -> table.getItems().add(new CountRow(String.valueOf(key), value)));

        pane.setCenter(chart);
        pane.setRight(table);
        pane.setBottom(new Label(getTrendText(counts)));
        return pane;
    }

    private BorderPane buildClusteringPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));

        thresholdField = new TextField("0.25");
        thresholdField.setPrefWidth(80);
        Button previewButton = new Button("Preview");
        Button saveButton = new Button("Save Classification");
        previewButton.setOnAction(e -> previewClustering());
        saveButton.setOnAction(e -> saveClustering());

        HBox controls = new HBox(8, new Label("Cosine threshold"), thresholdField, previewButton, saveButton);

        clusteringTable = new TableView<>();
        TableColumn<ClusterRow, Integer> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        TableColumn<ClusterRow, String> keywordColumn = new TableColumn<>("Keywords");
        keywordColumn.setCellValueFactory(new PropertyValueFactory<>("keywords"));
        TableColumn<ClusterRow, Integer> countColumn = new TableColumn<>("Issue Count");
        countColumn.setCellValueFactory(new PropertyValueFactory<>("issueCount"));
        TableColumn<ClusterRow, String> issueColumn = new TableColumn<>("Issues");
        issueColumn.setCellValueFactory(new PropertyValueFactory<>("issues"));
        clusteringTable.getColumns().add(categoryColumn);
        clusteringTable.getColumns().add(keywordColumn);
        clusteringTable.getColumns().add(countColumn);
        clusteringTable.getColumns().add(issueColumn);

        pane.setTop(controls);
        pane.setCenter(clusteringTable);
        pane.setBottom(new Label("Preview uses TF-IDF keywords and cosine similarity; save applies the shown classification."));
        return pane;
    }

    private void previewClustering() {
        try {
            double threshold = readThreshold();
            previewCategories = categoryController.previewCategories(project, threshold, currentUser);
            renderCategories(previewCategories);
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void saveClustering() {
        try {
            double threshold = readThreshold();
            if (previewCategories == null || previewCategories.isEmpty()) {
                previewCategories = categoryController.previewCategories(project, threshold, currentUser);
                renderCategories(previewCategories);
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Save this classification to the project?", ButtonType.YES, ButtonType.NO);
            confirm.initOwner(owner);
            confirm.showAndWait().ifPresent(buttonType -> {
                if (buttonType != ButtonType.YES) {
                    return;
                }
                List<Category> savedCategories = categoryController.createCategories(project, threshold, currentUser);
                previewCategories = savedCategories;
                renderCategories(savedCategories);
            });
        } catch (RuntimeException e) {
            showError(e.getMessage());
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
        clusteringTable.getItems().clear();
        if (categories == null) {
            return;
        }

        for (Category category : categories) {
            String issues = category.getIssues().stream()
                    .map(issue -> "#" + issue.getIssueId() + " " + issue.getTitle())
                    .collect(Collectors.joining(", "));
            clusteringTable.getItems().add(new ClusterRow(
                    category.getCategoryId(),
                    getTopKeywords(category, 5),
                    category.getIssues().size(),
                    issues));
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    private HBox buildSummaryPane() {
        TableView<CountRow> priorityTable = new TableView<>();
        addSummaryColumns(priorityTable, "Priority", "Issues");
        for (Map.Entry<Priority, Long> entry
                : statisticsController.getIssueCountByPriority(project.getProjectId()).entrySet()) {
            priorityTable.getItems().add(new CountRow(entry.getKey().name(), entry.getValue()));
        }

        TableView<CountRow> developerTable = new TableView<>();
        addSummaryColumns(developerTable, "Developer", "Resolved");
        for (Map.Entry<User, Long> entry
                : statisticsController.getResolvedCountByDeveloper(project.getProjectId()).entrySet()) {
            String loginId = entry.getKey() != null ? entry.getKey().getLoginId() : "-";
            developerTable.getItems().add(new CountRow(loginId, entry.getValue()));
        }

        HBox pane = new HBox(8, priorityTable, developerTable);
        pane.setPadding(new Insets(8));
        HBox.setHgrow(priorityTable, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(developerTable, javafx.scene.layout.Priority.ALWAYS);
        return pane;
    }

    private void addSummaryColumns(TableView<CountRow> table, String label, String count) {
        TableColumn<CountRow, String> labelColumn = new TableColumn<>(label);
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));
        TableColumn<CountRow, Long> countColumn = new TableColumn<>(count);
        countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        table.getColumns().add(labelColumn);
        table.getColumns().add(countColumn);
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

    public static class CountRow {
        private final String label;
        private final Long count;

        public CountRow(String label, Long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public Long getCount() {
            return count;
        }
    }

    public static class ClusterRow {
        private final Integer categoryId;
        private final String keywords;
        private final Integer issueCount;
        private final String issues;

        public ClusterRow(Integer categoryId, String keywords, Integer issueCount, String issues) {
            this.categoryId = categoryId;
            this.keywords = keywords;
            this.issueCount = issueCount;
            this.issues = issues;
        }

        public Integer getCategoryId() {
            return categoryId;
        }

        public String getKeywords() {
            return keywords;
        }

        public Integer getIssueCount() {
            return issueCount;
        }

        public String getIssues() {
            return issues;
        }
    }
}
