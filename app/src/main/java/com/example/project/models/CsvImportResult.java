package com.example.project.models;

import java.util.ArrayList;
import java.util.List;

public class CsvImportResult {
    private int insertedCount;
    private int updatedCount;
    private int rejectedCount;
    private final List<String> rejectedReasons = new ArrayList<>();

    public CsvImportResult() {
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public void incrementInserted() {
        this.insertedCount++;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public void addRejectedReason(String reason) {
        this.rejectedReasons.add(reason);
        this.rejectedCount++;
    }

    public List<String> getRejectedReasons() {
        return rejectedReasons;
    }
}
