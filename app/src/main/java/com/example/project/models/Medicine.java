package com.example.project.models;

public class Medicine {
    private String id;
    private String name;
    private String genericName;
    private String description;
    private double price;
    private int stock;
    private String imageUrl;
    private boolean active;
    private boolean requiresPrescription;
    private String category;
    private long createdAt;
    private long updatedAt;

    public Medicine() {
        // Required for Firestore
        this.active = true;
    }

    public Medicine(String id, String name, String genericName, String description,
                    double price, int stock, String imageUrl, boolean active,
                    boolean requiresPrescription, String category,
                    long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.active = active;
        this.requiresPrescription = requiresPrescription;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAvailable() {
        return active && stock > 0;
    }
}
