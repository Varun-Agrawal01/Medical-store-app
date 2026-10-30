package com.example.project.utils;

import java.util.regex.Pattern;

/**
 * Validation utilities for phone numbers, postal pincodes, emails, and passwords.
 */
public final class Validators {

    // Indian mobile phone number: exactly 10 digits, starting with 6, 7, 8, or 9
    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    // Indian postal pincode: 6 digits, first digit between 1 and 9
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9]\\d{5}$");

    // Standard email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private Validators() {
        // Utility class
    }

    /**
     * Validates Indian 10-digit mobile phone number starting with 6, 7, 8, or 9.
     */
    public static boolean isValidIndianPhone(String phone) {
        if (phone == null) return false;
        String trimmed = phone.trim();
        return INDIAN_PHONE_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates Indian 6-digit postal code.
     */
    public static boolean isValidPincode(String pincode) {
        if (pincode == null) return false;
        String trimmed = pincode.trim();
        return PINCODE_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates email address format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Password strength requirement:
     * - At least 8 characters in length
     * - At least one letter (a-z or A-Z)
     * - At least one numeric digit (0-9)
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }
}
