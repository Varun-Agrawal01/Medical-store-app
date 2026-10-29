package com.example.project;

import com.example.project.utils.PasswordHasher;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordHasherTest {

    @Test
    public void testSamePasswordWithDifferentSaltsGivesDifferentHashes() {
        char[] password = "secretPassword123!".toCharArray();

        byte[] salt1 = PasswordHasher.generateSalt();
        byte[] salt2 = PasswordHasher.generateSalt();

        assertNotEquals("Generated salts must be random and unique",
                new String(salt1), new String(salt2));

        String hash1 = PasswordHasher.hashWithSalt(password, salt1);
        String hash2 = PasswordHasher.hashWithSalt(password, salt2);

        assertNotEquals("Hashes with different salts must not be equal", hash1, hash2);

        // However, both must verify successfully against the password
        assertTrue(PasswordHasher.verify(password, hash1));
        assertTrue(PasswordHasher.verify(password, hash2));
    }

    @Test
    public void testVerifySucceedsForRightPasswordAndFailsForWrongOne() {
        char[] correctPassword = "CorrectHorseBatteryStaple".toCharArray();
        char[] wrongPassword = "IncorrectPassword123".toCharArray();

        String hash = PasswordHasher.hash(correctPassword);
        assertNotNull(hash);

        assertTrue("Verify must succeed for right password",
                PasswordHasher.verify(correctPassword, hash));

        assertFalse("Verify must fail for wrong password",
                PasswordHasher.verify(wrongPassword, hash));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPasswordIsRejected() {
        PasswordHasher.hash(new char[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPasswordIsRejected() {
        PasswordHasher.hash(null);
    }

    @Test
    public void testVerifyHandlesInvalidInputsGracefully() {
        assertFalse(PasswordHasher.verify(null, "somehash"));
        assertFalse(PasswordHasher.verify("pass".toCharArray(), null));
        assertFalse(PasswordHasher.verify(new char[0], "somehash"));
        assertFalse(PasswordHasher.verify("pass".toCharArray(), "invalid_hash_format_without_dollar"));
    }
}
