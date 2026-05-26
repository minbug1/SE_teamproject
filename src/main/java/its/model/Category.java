package its.model;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private int categoryId;
    private String name;
    private List<String> representativeWords = new ArrayList<>();

    public Category(int categoryId, String name) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name must not be empty.");
        }

        this.categoryId = categoryId;
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public List<String> getRepresentativeWords() {
        return new ArrayList<>(representativeWords);
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name must not be empty.");
        }

        this.name = name;
    }

    public void setRepresentativeWords(List<String> representativeWords) {
        if (representativeWords == null) {
            this.representativeWords = new ArrayList<>();
            return;
        }

        this.representativeWords = new ArrayList<>(representativeWords);
    }

    @Override
    public String toString() {
        return "Category{" +
                "categoryId=" + categoryId +
                ", name='" + name + '\'' +
                ", representativeWords=" + representativeWords +
                '}';
    }
}