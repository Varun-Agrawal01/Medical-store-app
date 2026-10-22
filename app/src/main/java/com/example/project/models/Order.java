package com.example.project.models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private List<OrderItem> items = new ArrayList<>();
    private double subtotal;
    private double deliveryFee;
    private double total;
    private String status; // Pending, Confirmed, Preparing, Completed, Cancelled
    private long createdAt;
    private long updatedAt;
    private boolean stockDeducted;

    public Order() {
        // Required for Firestore
        this.status = "Pending";
        this.stockDeducted = true;
    }

    public Order(String orderId, String userId, String customerName, String customerPhone,
                 String deliveryAddress, List<OrderItem> items, double subtotal,
                 double deliveryFee, double total, String status, long createdAt,
                 long updatedAt, boolean stockDeducted) {
        this.orderId = orderId;
        this.userId = userId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.items = items;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stockDeducted = stockDeducted;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isStockDeducted() {
        return stockDeducted;
    }

    public void setStockDeducted(boolean stockDeducted) {
        this.stockDeducted = stockDeducted;
    }
}
