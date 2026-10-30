package com.example.project.models;

public class Category {
    private String id;
    private String name;
    private String description;
    private String iconResName;

    public Category() {
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Category(String id, String name, String description, String iconResName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResName = iconResName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getIconResName() {
        return iconResName;
    }

    public void setIconResName(String iconResName) {
        this.iconResName = iconResName;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
