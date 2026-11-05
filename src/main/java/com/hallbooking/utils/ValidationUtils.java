package com.hallbooking.utils;

/**
 * ValidationUtils — input validation helpers used in all controllers.
 * All methods return null if valid, or an error message string if invalid.
 */
public class ValidationUtils {

    // ── Not empty ─────────────────────────────────────────────────
    public static String requireNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty())
            return fieldName + " cannot be empty.";
        return null;
    }

    // ── Valid email ────────────────────────────────────────────────
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return "Email cannot be empty.";
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            return "Please enter a valid email address.";
        return null;
    }

    // ── Password length ────────────────────────────────────────────
    public static String validatePassword(String password) {
        if (password == null || password.trim().isEmpty())
            return "Password cannot be empty.";
        if (password.length() < 6)
            return "Password must be at least 6 characters.";
        return null;
    }

    // ── Passwords match ────────────────────────────────────────────
    public static String validatePasswordMatch(String password, String confirm) {
        if (!password.equals(confirm))
            return "Passwords do not match.";
        return null;
    }

    // ── Valid Malaysian phone ──────────────────────────────────────
    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty())
            return "Phone number cannot be empty.";
        if (!phone.matches("^0[0-9]{8,10}$"))
            return "Phone must be 9–11 digits starting with 0 (e.g. 0123456789).";
        return null;
    }

    // ── Positive integer ──────────────────────────────────────────
    public static String validatePositiveInt(String value, String fieldName) {
        try {
            int v = Integer.parseInt(value.trim());
            if (v <= 0) return fieldName + " must be a positive number.";
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid whole number.";
        }
        return null;
    }

    // ── Positive double ───────────────────────────────────────────
    public static String validatePositiveDouble(String value, String fieldName) {
        try {
            double v = Double.parseDouble(value.trim());
            if (v <= 0) return fieldName + " must be greater than zero.";
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid number (e.g. 300.00).";
        }
        return null;
    }

    // ── No pipe characters (would corrupt the file format) ────────
    public static String validateNoPipe(String value, String fieldName) {
        if (value != null && value.contains("|"))
            return fieldName + " must not contain the | character.";
        return null;
    }

    // ── Combine multiple checks: returns first error found ─────────
    public static String first(String... results) {
        for (String r : results) {
            if (r != null) return r;
        }
        return null;
    }
}
