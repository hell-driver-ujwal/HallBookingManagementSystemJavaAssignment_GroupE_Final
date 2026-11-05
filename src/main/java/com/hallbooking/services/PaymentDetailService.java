package com.hallbooking.services;

import com.hallbooking.models.PaymentDetail;
import com.hallbooking.utils.FileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentDetailService — saves and retrieves extra payment details
 * for Credit Card and Online Transfer transactions.
 *
 * CVV is never stored. Card numbers are masked to last 4 digits.
 */
public class PaymentDetailService {

    // ── Malaysian bank detection by first 2 digits of 14-digit account ──
    // Based on common Malaysian bank account prefixes
    public static String detectBank(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 2) return "Unknown Bank";
        String prefix = accountNumber.substring(0, 2);
        return switch (prefix) {
            case "11" -> "Maybank (Malayan Banking)";
            case "12" -> "CIMB Bank";
            case "14" -> "Public Bank";
            case "15" -> "RHB Bank";
            case "16" -> "Hong Leong Bank";
            case "17" -> "AmBank";
            case "18" -> "Bank Rakyat";
            case "19" -> "OCBC Bank Malaysia";
            case "20" -> "HSBC Bank Malaysia";
            case "21" -> "Standard Chartered Malaysia";
            case "22" -> "Alliance Bank";
            case "23" -> "Affin Bank";
            case "24" -> "Bank Islam Malaysia";
            case "25" -> "Bank Muamalat";
            case "26" -> "UOB Malaysia";
            case "27" -> "Citibank Malaysia";
            case "28" -> "BSN (Bank Simpanan Nasional)";
            case "29" -> "Agrobank";
            default   -> "Unknown Bank";
        };
    }

    // ── Generate a unique reference number for Online Transfer ──────
    // Format: HBS-YYYYMMDD-XXXXXX  (HBS = Hall Booking System)
    public static String generateReferenceNumber() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%06d", (int)(Math.random() * 999999));
        return "HBS-" + date + "-" + rand;
    }

    // ── Our service's bank account (shown to customer for transfer) ──
    public static final String SERVICE_BANK_NAME    = "Maybank (Malayan Banking)";
    public static final String SERVICE_ACCOUNT_NAME = "Hall Symphony Sdn Bhd";
    public static final String SERVICE_ACCOUNT_NO   = "11223344556677";

    // ── Mask card number — keep only last 4 digits ───────────────────
    public static String maskCardNumber(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s+", "");
        if (digits.length() < 4) return "****";
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    // ── Save Credit Card payment detail ──────────────────────────────
    public String saveCreditCardDetail(String paymentId, String cardholderName,
                                        String rawCardNumber, String expiry) {
        if (cardholderName.trim().isEmpty()) return "Cardholder name is required.";
        String digits = rawCardNumber.replaceAll("\\s+", "");
        if (!digits.matches("\\d{16}"))     return "Card number must be exactly 16 digits.";
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}"))
            return "Expiry must be in MM/YY format (e.g. 08/27).";
        if (isCardExpired(expiry))          return "This card has expired.";

        String detailId = FileManager.getNextId("PD", FileManager.PAYMENT_DETAILS);
        PaymentDetail detail = new PaymentDetail(
                detailId, paymentId, PaymentDetail.CREDIT_CARD,
                cardholderName.trim(),
                maskCardNumber(digits),
                expiry.trim());
        FileManager.appendLine(FileManager.PAYMENT_DETAILS, detail.toFileString());
        return null; // success
    }

    // ── Save Online Transfer payment detail ──────────────────────────
    public String saveOnlineTransferDetail(String paymentId, String accountName,
                                            String bankName, String referenceNumber,
                                            double bookingAmount, double confirmedAmount) {
        if (accountName.trim().isEmpty())    return "Account name is required.";
        if (bankName.trim().isEmpty())       return "Bank name is required.";
        if (referenceNumber.trim().isEmpty()) return "Reference number is required.";
        if (Math.abs(confirmedAmount - bookingAmount) > 0.01)
            return String.format("Amount confirmation does not match.\n"
                    + "Expected: RM %.2f  —  You entered: RM %.2f", bookingAmount, confirmedAmount);

        String detailId = FileManager.getNextId("PD", FileManager.PAYMENT_DETAILS);
        PaymentDetail detail = new PaymentDetail(
                detailId, paymentId, PaymentDetail.ONLINE_TRANSFER,
                accountName.trim(),
                bankName.trim(),
                referenceNumber.trim());
        FileManager.appendLine(FileManager.PAYMENT_DETAILS, detail.toFileString());
        return null; // success
    }

    // ── Lookup ────────────────────────────────────────────────────────
    public PaymentDetail getDetailByPayment(String paymentId) {
        List<String> lines = FileManager.findByField(FileManager.PAYMENT_DETAILS, 1, paymentId);
        return lines.isEmpty() ? null : PaymentDetail.fromString(lines.get(0));
    }

    public List<PaymentDetail> getAllDetails() {
        List<PaymentDetail> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.PAYMENT_DETAILS))
            list.add(PaymentDetail.fromString(line));
        return list;
    }

    // ── Helper: check if card is expired ─────────────────────────────
    private boolean isCardExpired(String expiry) {
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year  = 2000 + Integer.parseInt(parts[1]);
            java.time.YearMonth cardYM = java.time.YearMonth.of(year, month);
            return cardYM.isBefore(java.time.YearMonth.now());
        } catch (Exception e) {
            return true;
        }
    }
}
