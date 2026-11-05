package com.hallbooking.models;

public class Booking {
    private String bookingId;
    private String customerId;
    private String hallId;
    private String availId;       // "NULL" if slot not linked
    private String eventStart;
    private String eventEnd;
    private double totalHours;
    private double totalAmount;
    private String status;        // CONFIRMED, CANCELLED
    private String bookedAt;
    private String cancelledAt;   // "NULL" if not cancelled

    public Booking() {}

    public Booking(String bookingId, String customerId, String hallId, String availId,
                   String eventStart, String eventEnd, double totalHours, double totalAmount,
                   String status, String bookedAt, String cancelledAt) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.hallId = hallId;
        this.availId = availId;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.totalHours = totalHours;
        this.totalAmount = totalAmount;
        this.status = status;
        this.bookedAt = bookedAt;
        this.cancelledAt = cancelledAt;
    }

    public static Booking fromString(String line) {
        String[] p = line.split("\\|");
        return new Booking(p[0], p[1], p[2], p[3], p[4], p[5],
                Double.parseDouble(p[6]), Double.parseDouble(p[7]),
                p[8], p[9], p[10]);
    }

    public String toFileString() {
        return String.join("|", bookingId, customerId, hallId, availId,
                eventStart, eventEnd,
                String.valueOf((int) totalHours),
                String.format("%.2f", totalAmount),
                status, bookedAt, cancelledAt);
    }

    public String getBookingId()   { return bookingId; }
    public String getCustomerId()  { return customerId; }
    public String getHallId()      { return hallId; }
    public String getAvailId()     { return availId; }
    public String getEventStart()  { return eventStart; }
    public String getEventEnd()    { return eventEnd; }
    public double getTotalHours()  { return totalHours; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus()      { return status; }
    public String getBookedAt()    { return bookedAt; }
    public String getCancelledAt() { return cancelledAt; }

    public void setBookingId(String bookingId)     { this.bookingId = bookingId; }
    public void setCustomerId(String customerId)   { this.customerId = customerId; }
    public void setHallId(String hallId)           { this.hallId = hallId; }
    public void setAvailId(String availId)         { this.availId = availId; }
    public void setEventStart(String eventStart)   { this.eventStart = eventStart; }
    public void setEventEnd(String eventEnd)       { this.eventEnd = eventEnd; }
    public void setTotalHours(double totalHours)   { this.totalHours = totalHours; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status)           { this.status = status; }
    public void setBookedAt(String bookedAt)       { this.bookedAt = bookedAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
}
