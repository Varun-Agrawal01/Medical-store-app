package com.example.project.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 password hasher using PBKDF2WithHmacSHA256.
 * Salt: 16 random bytes.
 * Iterations: 120,000.
 * Output: Base64(salt) + "$" + Base64(hash)
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256; // bits

    private PasswordHasher() {
        // Utility class
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    public static String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return hashWithSalt(password, generateSalt());
    }

    public static String hashWithSalt(char[] password, byte[] salt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        Objects.requireNonNull(salt, "Salt cannot be null");
        try {
            byte[] hash = pbkdf2(password, salt);
            return encodeBase64(salt) + "$" + encodeBase64(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    public static boolean verify(char[] password, String storedHash) {
        if (password == null || password.length == 0 || storedHash == null) {
            return false;
        }
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) {
                return false;
            }
            byte[] salt = decodeBase64(parts[0]);
            byte[] expectedHash = decodeBase64(parts[1]);
            byte[] actualHash = pbkdf2(password, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Pure Java Base64 utility that works seamlessly in both standard JVM JUnit tests and Android runtime
    private static final char[] B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static String encodeBase64(byte[] data) {
        int len = data.length;
        StringBuilder sb = new StringBuilder(((len + 2) / 3) * 4);
        for (int i = 0; i < len; i += 3) {
            int b0 = data[i] & 0xFF;
            int b1 = (i + 1 < len) ? data[i + 1] & 0xFF : 0;
            int b2 = (i + 2 < len) ? data[i + 2] & 0xFF : 0;

            sb.append(B64_CHARS[b0 >>> 2]);
            sb.append(B64_CHARS[((b0 & 0x03) << 4) | (b1 >>> 4)]);
            if (i + 1 < len) {
                sb.append(B64_CHARS[((b1 & 0x0F) << 2) | (b2 >>> 6)]);
            } else {
                sb.append('=');
            }
            if (i + 2 < len) {
                sb.append(B64_CHARS[b2 & 0x3F]);
            } else {
                sb.append('=');
            }
        }
        return sb.toString();
    }

    private static byte[] decodeBase64(String s) {
        // Strip padding and map
        int pad = 0;
        int len = s.length();
        while (len > 0 && s.charAt(len - 1) == '=') {
            pad++;
            len--;
        }
        int outLen = (s.length() * 3) / 4 - pad;
        byte[] out = new byte[outLen];
        int outIdx = 0;
        int accum = 0;
        int bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int val;
            if (c >= 'A' && c <= 'Z') val = c - 'A';
            else if (c >= 'a' && c <= 'z') val = c - 'a' + 26;
            else if (c >= '0' && c <= '9') val = c - '0' + 52;
            else if (c == '+') val = 62;
            else if (c == '/') val = 63;
            else if (c == '=') break;
            else continue;

            accum = (accum << 6) | val;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                if (outIdx < outLen) {
                    out[outIdx++] = (byte) ((accum >>> bits) & 0xFF);
                }
            }
        }
        return out;
    }
}
