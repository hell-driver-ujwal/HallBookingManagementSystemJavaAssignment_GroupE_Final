package com.hallbooking.utils;

/**
 * PasswordUtils — simple password helpers.
 * Since the assignment uses .txt files (no real DB), we store passwords as-is.
 * In a real system you would hash with BCrypt. This class is structured so
 * you can easily swap in hashing later without changing any other code.
 */
public class PasswordUtils {

    // ── "Hash" a password (plain text for now, swap for BCrypt later)
    public static String hash(String plainText) {
        // TODO: replace with BCrypt.hashpw(plainText, BCrypt.gensalt()) for production
        return plainText;
    }

    // ── Verify a plain-text password against a stored value
    public static boolean verify(String plainText, String stored) {
        // TODO: replace with BCrypt.checkpw(plainText, stored) for production
        return plainText.equals(stored);
    }
}
