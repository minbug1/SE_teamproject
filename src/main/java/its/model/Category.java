package its.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * model for Category
 *
 * 
 * @author hanung
 */

public class Category {

    private int projectId;
    private int categoryId;
    private String categoryName;
    // threshold
    private double threshold = 0.25;
    // issues
    private List<Issue> issues = new ArrayList<>();
    // category vocabulary vector
    private Map<String, Double> representVector = new HashMap<>();
    // idf vector
    private Map<String, Double> idfVector = new HashMap<>();

    // constructor
    public Category(int projectId, int categoryId, double cosineThreshold, List<Issue> issues, Map<String, Double> representVector, Map<String, Double> idfVector) {
        this.projectId = projectId;
        this.categoryId = categoryId;
        this.threshold = cosineThreshold;
        this.issues = issues != null ? issues : new ArrayList<>();
        this.representVector = representVector != null ? representVector : new HashMap<>();
        this.idfVector = idfVector != null ? idfVector : new HashMap<>();
    }

    public String getCategoryName() {
        if (this.categoryName != null && !this.categoryName.trim().isEmpty()) {
            return this.categoryName;
        }
        
        if (this.representVector != null && !this.representVector.isEmpty()) {
            String bestWord = null;
            double maxWeight = -1.0;

            for (Map.Entry<String, Double> entry : this.representVector.entrySet()) {
                if (entry.getValue() > maxWeight) {
                    maxWeight = entry.getValue();
                    bestWord = entry.getKey();
                }
            }

            if (bestWord != null) {
                return bestWord;
            }
        }

        return "Unassigned_Category_" + this.categoryId;
    }

    public int getCategoryId() {
        return categoryId;
    }
    
    public int getProjectId() {
        return projectId;
    }
    
    public double getThreshold() {
        return threshold;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public Map<String, Double> getRepresentVector() {
        return representVector;
    }

    public Map<String, Double> getIdf() {
        return idfVector;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}