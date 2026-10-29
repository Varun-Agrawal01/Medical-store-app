package com.example.project.models;

public class MedicineBatch {
    private String id;
    private String medicineId;
    private String batchNo;
    private String expiryDate; // YYYY-MM-DD or YYYY-MM
    private int quantity;

    public MedicineBatch() {
    }

    public MedicineBatch(String id, String medicineId, String batchNo, String expiryDate, int quantity) {
        this.id = id;
        this.medicineId = medicineId;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
