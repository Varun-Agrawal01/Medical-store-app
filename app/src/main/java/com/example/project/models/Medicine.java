package com.example.project.models;

import java.util.ArrayList;
import java.util.List;

public class Medicine {
    private String id;
    private String name;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String packSize;
    private String dosageForm;
    private String description;
    private String usageInfo;
    private String sideEffects;
    private double mrp;
    private double price;
    private double gstRate;
    private int stock;
    private boolean active;
    private boolean requiresPrescription;
    private String category;
    private String imageUrl;
    private int minAge;
    private int avoidInPregnancy;
    private int matchedSymptomsCount;
    /** Transient: symptom IDs selected in the admin Add/Edit form. Not persisted as a column. */
    private List<String> selectedSymptomIds = new ArrayList<>();
    private long createdAt;
    private long updatedAt;

    public Medicine() {
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
        this.mrp = price;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getPackSize() {
        return packSize;
    }

    public void setPackSize(String packSize) {
        this.packSize = packSize;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsageInfo() {
        return usageInfo;
    }

    public void setUsageInfo(String usageInfo) {
        this.usageInfo = usageInfo;
    }

    public String getSideEffects() {
        return sideEffects;
    }

    public void setSideEffects(String sideEffects) {
        this.sideEffects = sideEffects;
    }

    public double getMrp() {
        return mrp;
    }

    public void setMrp(double mrp) {
        this.mrp = mrp;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getGstRate() {
        return gstRate;
    }

    public void setGstRate(double gstRate) {
        this.gstRate = gstRate;
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

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getAvoidInPregnancy() {
        return avoidInPregnancy;
    }

    public void setAvoidInPregnancy(int avoidInPregnancy) {
        this.avoidInPregnancy = avoidInPregnancy;
    }

    public int getMatchedSymptomsCount() {
        return matchedSymptomsCount;
    }

    public void setMatchedSymptomsCount(int matchedSymptomsCount) {
        this.matchedSymptomsCount = matchedSymptomsCount;
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

    public List<String> getSelectedSymptomIds() {
        return selectedSymptomIds != null ? selectedSymptomIds : new ArrayList<>();
    }

    public void setSelectedSymptomIds(List<String> selectedSymptomIds) {
        this.selectedSymptomIds = selectedSymptomIds != null ? selectedSymptomIds : new ArrayList<>();
    }

    public boolean isAvailable() {
        return active && stock > 0;
    }
}
