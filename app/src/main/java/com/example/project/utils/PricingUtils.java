package com.example.project.utils;

/**
 * Centralized business logic for pricing, delivery fee, and GST calculations.
 */
public final class PricingUtils {

    public static final double FREE_DELIVERY_THRESHOLD = 500.0;
    public static final double STANDARD_DELIVERY_FEE = 50.0;

    private PricingUtils() {
        // Utility class
    }

    /**
     * Delivery fee rule: Free above Rs 500, else Rs 50 (or Rs 0 if cart/subtotal is 0).
     */
    public static double calculateDeliveryFee(double subtotal) {
        if (subtotal <= 0.0 || subtotal >= FREE_DELIVERY_THRESHOLD) {
            return 0.0;
        }
        return STANDARD_DELIVERY_FEE;
    }

    /**
     * Calculates GST portion for a given base amount and rate (5% or 12%).
     */
    public static double calculateGstAmount(double amount, double gstRate) {
        if (amount <= 0.0 || gstRate <= 0.0) {
            return 0.0;
        }
        return (amount * gstRate) / 100.0;
    }

    /**
     * Calculates total = subtotal + delivery fee.
     */
    public static double calculateTotal(double subtotal, double deliveryFee) {
        return subtotal + deliveryFee;
    }
}
