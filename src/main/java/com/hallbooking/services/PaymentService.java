package com.hallbooking.services;

import com.hallbooking.models.Payment;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentService — records and retrieves payments.
 *
 * Functions:
 *   recordPayment(bookingId, amount, method)  → called right after createBooking()
 *   getPaymentByBooking(bookingId)            → customer receipt lookup
 *   getAllPayments()                           → admin/manager view
 *   getPaymentById(paymentId)                 → single lookup
 */
public class PaymentService {

    public String recordPayment(String bookingId, double amount, String method) {
        if (method.trim().isEmpty()) return "Payment method is required.";
        if (amount <= 0)             return "Payment amount must be greater than zero.";

        String paymentId     = FileManager.getNextId("P", FileManager.PAYMENTS);
        String receiptNumber = DateTimeUtils.generateReceiptNumber();
        Payment payment = new Payment(paymentId, bookingId, amount,
                method, DateTimeUtils.now(), receiptNumber);
        FileManager.appendLine(FileManager.PAYMENTS, payment.toFileString());
        return null;
    }

    public Payment getPaymentByBooking(String bookingId) {
        List<String> lines = FileManager.findByField(FileManager.PAYMENTS, 1, bookingId);
        return lines.isEmpty() ? null : Payment.fromString(lines.get(0));
    }

    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.PAYMENTS))
            list.add(Payment.fromString(line));
        return list;
    }

    public Payment getPaymentById(String paymentId) {
        String line = FileManager.findById(FileManager.PAYMENTS, paymentId);
        return line != null ? Payment.fromString(line) : null;
    }
}
