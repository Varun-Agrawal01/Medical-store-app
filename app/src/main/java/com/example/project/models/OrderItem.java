package com.example.project.models;

public class OrderItem {
    private String medicineId;
    private String medicineName;
    private double unitPrice;
    private int quantity;
    private String batchNo;

    public OrderItem() {
        // Default constructor
    }

    public OrderItem(String medicineId, String medicineName, double unitPrice, int quantity) {
        this(medicineId, medicineName, unitPrice, quantity, "");
    }

    public OrderItem(String medicineId, String medicineName, double unitPrice, int quantity, String batchNo) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.batchNo = batchNo;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public double getItemTotal() {
        return unitPrice * quantity;
    }
}
