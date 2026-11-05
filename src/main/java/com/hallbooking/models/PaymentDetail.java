package com.hallbooking.models;

/**
 * PaymentDetail — extra details for Credit Card or Online Transfer payments.
 *
 * Linked to a Payment record via paymentId.
 *
 * Format in payment_details.txt:
 *   detailId|paymentId|method|field1|field2|field3
 *
 *   Credit Card     → detailId|paymentId|Credit Card|cardholderName|maskedCardNumber|expiry
 *   Online Transfer → detailId|paymentId|Online Transfer|accountName|bankName|referenceNumber
 *
 * CVV is intentionally NOT stored (security).
 * Full card number is masked to last 4 digits before storing.
 */
public class PaymentDetail {

    public static final String CREDIT_CARD     = "Credit Card";
    public static final String ONLINE_TRANSFER = "Online Transfer";

    private String detailId;
    private String paymentId;
    private String method;
    private String field1;   // Credit Card: cardholderName  | Online Transfer: accountName
    private String field2;   // Credit Card: maskedCard      | Online Transfer: bankName
    private String field3;   // Credit Card: expiry          | Online Transfer: referenceNumber

    public PaymentDetail() {}

    public PaymentDetail(String detailId, String paymentId, String method,
                         String field1, String field2, String field3) {
        this.detailId  = detailId;
        this.paymentId = paymentId;
        this.method    = method;
        this.field1    = field1;
        this.field2    = field2;
        this.field3    = field3;
    }

    public static PaymentDetail fromString(String line) {
        String[] p = line.split("\\|");
        return new PaymentDetail(p[0], p[1], p[2], p[3], p[4], p[5]);
    }

    public String toFileString() {
        return String.join("|", detailId, paymentId, method, field1, field2, field3);
    }

    // ── Getters ───────────────────────────────────────────────────
    public String getDetailId()  { return detailId; }
    public String getPaymentId() { return paymentId; }
    public String getMethod()    { return method; }
    public String getField1()    { return field1; }
    public String getField2()    { return field2; }
    public String getField3()    { return field3; }

    // ── Setters ───────────────────────────────────────────────────
    public void setDetailId(String detailId)   { this.detailId  = detailId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setMethod(String method)       { this.method    = method; }
    public void setField1(String field1)       { this.field1    = field1; }
    public void setField2(String field2)       { this.field2    = field2; }
    public void setField3(String field3)       { this.field3    = field3; }

    // ── Convenience getters by role ───────────────────────────────
    public String getCardholderName()   { return field1; } // Credit Card
    public String getMaskedCard()       { return field2; } // Credit Card
    public String getExpiry()           { return field3; } // Credit Card

    public String getAccountName()      { return field1; } // Online Transfer
    public String getBankName()         { return field2; } // Online Transfer
    public String getReferenceNumber()  { return field3; } // Online Transfer
}
