package com.hallbooking.models;

public class Payment {
    private String paymentId;
    private String bookingId;
    private double amount;
    private String paymentMethod; // Cash, Credit Card, Online Transfer
    private String paymentDate;
    private String receiptNumber;

    public Payment() {}

    public Payment(String paymentId, String bookingId, double amount,
                   String paymentMethod, String paymentDate, String receiptNumber) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.receiptNumber = receiptNumber;
    }

    public static Payment fromString(String line) {
        String[] p = line.split("\\|");
        return new Payment(p[0], p[1], Double.parseDouble(p[2]), p[3], p[4], p[5]);
    }

    public String toFileString() {
        return String.join("|", paymentId, bookingId,
                String.format("%.2f", amount), paymentMethod, paymentDate, receiptNumber);
    }

    public String getPaymentId()     { return paymentId; }
    public String getBookingId()     { return bookingId; }
    public double getAmount()        { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentDate()   { return paymentDate; }
    public String getReceiptNumber() { return receiptNumber; }

    public void setPaymentId(String paymentId)         { this.paymentId = paymentId; }
    public void setBookingId(String bookingId)         { this.bookingId = bookingId; }
    public void setAmount(double amount)               { this.amount = amount; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentDate(String paymentDate)     { this.paymentDate = paymentDate; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
}
