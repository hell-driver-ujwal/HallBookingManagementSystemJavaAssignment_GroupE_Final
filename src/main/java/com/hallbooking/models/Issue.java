package com.hallbooking.models;

/**
 * Issue — a complaint or problem reported by a customer about a hall.
 * Extends HallEvent, inheriting hallId and status.
 *
 * Status values  : OPEN, IN_PROGRESS, DONE, CLOSED, CANCELLED
 * Priority values: LOW, MEDIUM, HIGH
 *
 * Format in issues.txt:
 *   issueId|customerId|bookingId|hallId|title|description|reportedAt|status|priority
 *
 * Priority is optional — defaults to MEDIUM for backward compatibility
 * with old records that have only 8 fields.
 */
public class Issue extends HallEvent {

    private String issueId;
    private String customerId;
    private String bookingId;
    private String title;
    private String description;
    private String reportedAt;
    private String priority; // LOW, MEDIUM, HIGH

    public Issue() {}

    public Issue(String issueId, String customerId, String bookingId, String hallId,
                 String title, String description, String reportedAt,
                 String status, String priority) {
        super(hallId, status);
        this.issueId     = issueId;
        this.customerId  = customerId;
        this.bookingId   = bookingId;
        this.title       = title;
        this.description = description;
        this.reportedAt  = reportedAt;
        this.priority    = priority != null ? priority : "MEDIUM";
    }

    public static Issue fromString(String line) {
        String[] p = line.split("\\|");
        // Backward compatible — old records have 8 fields, new have 9
        String priority = p.length >= 9 ? p[8] : "MEDIUM";
        return new Issue(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], priority);
    }

    @Override
    public String toFileString() {
        return String.join("|", issueId, customerId, bookingId, hallId,
                title, description, reportedAt, status,
                priority != null ? priority : "MEDIUM");
    }

    @Override
    public boolean isResolved() {
        return "DONE".equals(status)
            || "CLOSED".equals(status)
            || "CANCELLED".equals(status);
    }

    /** Returns true if this issue is overdue (IN_PROGRESS for more than 3 days) */
    public boolean isOverdue() {
        if (!"IN_PROGRESS".equals(status)) return false;
        try {
            java.time.LocalDateTime reported =
                    com.hallbooking.utils.DateTimeUtils.parse(reportedAt);
            return reported.isBefore(java.time.LocalDateTime.now().minusDays(3));
        } catch (Exception e) {
            return false;
        }
    }

    /** Priority sort order — HIGH=0, MEDIUM=1, LOW=2 */
    public int getPriorityOrder() {
        return switch (priority != null ? priority : "MEDIUM") {
            case "HIGH"   -> 0;
            case "MEDIUM" -> 1;
            default       -> 2;
        };
    }

    // Getters
    public String getIssueId()     { return issueId; }
    public String getCustomerId()  { return customerId; }
    public String getBookingId()   { return bookingId; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getReportedAt()  { return reportedAt; }
    public String getPriority()    { return priority != null ? priority : "MEDIUM"; }

    // Setters
    public void setIssueId(String issueId)         { this.issueId     = issueId; }
    public void setCustomerId(String customerId)   { this.customerId  = customerId; }
    public void setBookingId(String bookingId)     { this.bookingId   = bookingId; }
    public void setTitle(String title)             { this.title       = title; }
    public void setDescription(String description) { this.description = description; }
    public void setReportedAt(String reportedAt)   { this.reportedAt  = reportedAt; }
    public void setPriority(String priority)       { this.priority    = priority; }

    @Override
    public String toString() {
        return "Issue{" + issueId + ", " + title + ", " + status + ", " + priority + "}";
    }
}
