package com.hallbooking.models;

/**
 * ActivityLog — records every significant action in the system.
 *
 * Format in activity_log.txt:
 *   logId|userId|role|action|details|timestamp
 *
 * Actions: LOGIN, LOGOUT, BOOKING_CREATED, BOOKING_CANCELLED,
 *          ISSUE_REPORTED, ISSUE_ASSIGNED, ISSUE_RESOLVED,
 *          STAFF_CREATED, STAFF_DELETED, USER_BLOCKED, USER_UNBLOCKED,
 *          PASSWORD_RESET, HALL_ADDED, HALL_UPDATED, MAINTENANCE_SCHEDULED
 */
public class ActivityLog {

    private String logId;
    private String userId;
    private String role;
    private String action;
    private String details;
    private String timestamp;

    public ActivityLog() {}

    public ActivityLog(String logId, String userId, String role,
                       String action, String details, String timestamp) {
        this.logId     = logId;
        this.userId    = userId;
        this.role      = role;
        this.action    = action;
        this.details   = details;
        this.timestamp = timestamp;
    }

    public static ActivityLog fromString(String line) {
        String[] p = line.split("\\|", 6);
        return new ActivityLog(p[0], p[1], p[2], p[3], p[4], p[5]);
    }

    public String toFileString() {
        // Replace any pipe in details to avoid corruption
        String safeDetails = details.replace("|", ";");
        return String.join("|", logId, userId, role, action, safeDetails, timestamp);
    }

    public String getLogId()     { return logId; }
    public String getUserId()    { return userId; }
    public String getRole()      { return role; }
    public String getAction()    { return action; }
    public String getDetails()   { return details; }
    public String getTimestamp() { return timestamp; }
}
