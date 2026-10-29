package com.example.project.models;

public class CartItem {
    private String medicineId;
    private String medicineName;
    private double price;
    private int quantity;
    private boolean requiresPrescription;

    public CartItem() {
        // Default constructor
    }

    public CartItem(String medicineId, String medicineName, double price, int quantity, boolean requiresPrescription) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.price = price;
        this.quantity = quantity;
        this.requiresPrescription = requiresPrescription;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public double getItemTotal() {
        return price * quantity;
    }
}

