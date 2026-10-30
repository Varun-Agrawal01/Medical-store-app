package com.example.project;

import com.example.project.utils.Validators;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValidatorsTest {

    @Test
    public void testIndianPhoneValidation() {
        // Valid 10-digit Indian mobile numbers starting with 6, 7, 8, 9
        assertTrue(Validators.isValidIndianPhone("9876543210"));
        assertTrue(Validators.isValidIndianPhone("8123456789"));
        assertTrue(Validators.isValidIndianPhone("7000000000"));
        assertTrue(Validators.isValidIndianPhone("6999999999"));

        // Invalid: Starts with 0, 1, 2, 3, 4, 5
        assertFalse(Validators.isValidIndianPhone("5123456789"));
        assertFalse(Validators.isValidIndianPhone("0123456789"));
        assertFalse(Validators.isValidIndianPhone("1234567890"));

        // Invalid: Wrong length
        assertFalse(Validators.isValidIndianPhone("987654321"));   // 9 digits
        assertFalse(Validators.isValidIndianPhone("98765432100")); // 11 digits

        // Invalid: Non-numeric, null, empty
        assertFalse(Validators.isValidIndianPhone("98765abcde"));
        assertFalse(Validators.isValidIndianPhone(""));
        assertFalse(Validators.isValidIndianPhone("          "));
        assertFalse(Validators.isValidIndianPhone(null));
    }

    @Test
    public void testPincodeValidation() {
        // Valid 6 digits
        assertTrue(Validators.isValidPincode("400001"));
        assertTrue(Validators.isValidPincode("110001"));
        assertTrue(Validators.isValidPincode("560001"));

        // Invalid: Wrong length
        assertFalse(Validators.isValidPincode("40001"));  // 5 digits
        assertFalse(Validators.isValidPincode("4000001")); // 7 digits

        // Invalid: Starts with 0
        assertFalse(Validators.isValidPincode("012345"));

        // Invalid: Letters, null, empty
        assertFalse(Validators.isValidPincode("40000A"));
        assertFalse(Validators.isValidPincode(""));
        assertFalse(Validators.isValidPincode(null));
    }

    @Test
    public void testEmailValidation() {
        // Valid emails
        assertTrue(Validators.isValidEmail("user@example.com"));
        assertTrue(Validators.isValidEmail("john.doe@medistore.in"));
        assertTrue(Validators.isValidEmail("customer_123@hospital.org"));

        // Invalid emails
        assertFalse(Validators.isValidEmail("user@"));
        assertFalse(Validators.isValidEmail("user@.com"));
        assertFalse(Validators.isValidEmail("@example.com"));
        assertFalse(Validators.isValidEmail("plainaddress"));
        assertFalse(Validators.isValidEmail("user@example..com"));
        assertFalse(Validators.isValidEmail(""));
        assertFalse(Validators.isValidEmail(null));
    }

    @Test
    public void testPasswordStrengthValidation() {
        // Valid: At least 8 characters, at least 1 letter, at least 1 digit
        assertTrue(Validators.isStrongPassword("pass1234"));
        assertTrue(Validators.isStrongPassword("Admin2026!"));
        assertTrue(Validators.isStrongPassword("MediStore99"));

        // Invalid: Less than 8 characters
        assertFalse(Validators.isStrongPassword("pass12"));
        assertFalse(Validators.isStrongPassword("Abc1"));

        // Invalid: Only letters
        assertFalse(Validators.isStrongPassword("abcdefgh"));
        assertFalse(Validators.isStrongPassword("PasswordOnly"));

        // Invalid: Only digits
        assertFalse(Validators.isStrongPassword("12345678"));

        // Invalid: Empty or null
        assertFalse(Validators.isStrongPassword(""));
        assertFalse(Validators.isStrongPassword(null));
    }
}
