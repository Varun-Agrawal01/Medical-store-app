package com.example.project.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized business logic for pricing, delivery fee, GST calculations, and amount formatting.
 */
public final class PricingUtils {

    public static final double FREE_DELIVERY_THRESHOLD = 500.0;
    public static final double STANDARD_DELIVERY_FEE = 50.0;

    private PricingUtils() {
        // Utility class
    }

    /**
     * Delivery fee rule: Free at or above Rs 500, else Rs 50 (or Rs 0 if cart/subtotal is 0).
     */
    public static double calculateDeliveryFee(double subtotal) {
        if (subtotal <= 0.0 || subtotal >= FREE_DELIVERY_THRESHOLD) {
            return 0.0;
        }
        return STANDARD_DELIVERY_FEE;
    }

    /**
     * Calculates GST portion for a given base amount and rate (e.g. 5% or 12%).
     */
    public static double calculateGstAmount(double amount, double gstRate) {
        if (amount <= 0.0 || gstRate <= 0.0) {
            return 0.0;
        }
        return roundToTwoDecimals((amount * gstRate) / 100.0);
    }

    /**
     * GST Split between CGST and SGST (50% each for intra-state Indian tax).
     */
    public static class GstSplit {
        private final double cgst;
        private final double sgst;
        private final double total;

        public GstSplit(double cgst, double sgst) {
            this.cgst = cgst;
            this.sgst = sgst;
            this.total = roundToTwoDecimals(cgst + sgst);
        }

        public double getCgst() {
            return cgst;
        }

        public double getSgst() {
            return sgst;
        }

        public double getTotal() {
            return total;
        }
    }

    public static GstSplit calculateGstSplit(double totalGst) {
        if (totalGst <= 0.0) {
            return new GstSplit(0.0, 0.0);
        }
        double half = roundToTwoDecimals(totalGst / 2.0);
        return new GstSplit(half, half);
    }

    /**
     * Rounds a double to two decimal places using standard HALF_UP rounding.
     */
    public static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Calculates total = subtotal + delivery fee.
     */
    public static double calculateTotal(double subtotal, double deliveryFee) {
        return roundToTwoDecimals(subtotal + deliveryFee);
    }

    /**
     * Converts a numeric monetary amount into words (Indian numbering system format).
     * E.g. 500.00 -> "Five Hundred Rupees Only"
     * 1234.50 -> "One Thousand Two Hundred Thirty Four Rupees and Fifty Paise Only"
     */
    public static String formatAmountInWords(double amount) {
        if (amount < 0) {
            return "Minus " + formatAmountInWords(-amount);
        }
        long rupees = (long) amount;
        long paise = Math.round((amount - rupees) * 100);

        if (rupees == 0 && paise == 0) {
            return "Zero Rupees Only";
        }

        StringBuilder words = new StringBuilder();
        if (rupees > 0) {
            words.append(convertNumberToWords(rupees)).append(" Rupees");
        }

        if (paise > 0) {
            if (rupees > 0) {
                words.append(" and ");
            }
            words.append(convertNumberToWords(paise)).append(" Paise");
        }

        words.append(" Only");
        return words.toString();
    }

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static String convertNumberToWords(long n) {
        if (n == 0) return "Zero";
        if (n < 20) return UNITS[(int) n];
        if (n < 100) return TENS[(int) n / 10] + ((n % 10 != 0) ? " " + UNITS[(int) n % 10] : "");
        if (n < 1000) return UNITS[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " " + convertNumberToWords(n % 100) : "");
        if (n < 100000) return convertNumberToWords(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertNumberToWords(n % 1000) : "");
        if (n < 10000000) return convertNumberToWords(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertNumberToWords(n % 100000) : "");
        return convertNumberToWords(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convertNumberToWords(n % 10000000) : "");
    }
}
