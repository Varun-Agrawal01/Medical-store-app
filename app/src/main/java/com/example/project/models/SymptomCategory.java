package com.example.project.models;

import java.util.ArrayList;
import java.util.List;

public class SymptomCategory {
    private String categoryId;
    private String name;
    private String description;
    private List<String> linkedMedicineIds = new ArrayList<>();

    public SymptomCategory() {
        // Default constructor
    }

    public SymptomCategory(String categoryId, String name, String description, List<String> linkedMedicineIds) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.linkedMedicineIds = linkedMedicineIds;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getLinkedMedicineIds() {
        return linkedMedicineIds;
    }

    public void setLinkedMedicineIds(List<String> linkedMedicineIds) {
        this.linkedMedicineIds = linkedMedicineIds;
    }
}

