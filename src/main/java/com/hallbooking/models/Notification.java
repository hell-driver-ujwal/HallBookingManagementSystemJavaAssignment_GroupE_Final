package com.hallbooking.models;

/**
 * In-app notification stored in notifications.txt.
 *
 * Format:
 *   notifId | recipientId | senderRole | issueId | assignId | message | createdAt | read
 *
 * senderRole : MANAGER | SCHEDULER
 * read       : true | false
 */
public class Notification {

    private String notifId;
    private String recipientId;  // staffId or customerId
    private String senderRole;   // "MANAGER" or "SCHEDULER"
    private String issueId;
    private String assignId;
    private String message;
    private String createdAt;
    private boolean read;

    public Notification() {}

    public Notification(String notifId, String recipientId, String senderRole,
                        String issueId, String assignId,
                        String message, String createdAt, boolean read) {
        this.notifId     = notifId;
        this.recipientId = recipientId;
        this.senderRole  = senderRole;
        this.issueId     = issueId;
        this.assignId    = assignId;
        this.message     = message;
        this.createdAt   = createdAt;
        this.read        = read;
    }

    public static Notification fromString(String line) {
        String[] p = line.split("\\|", -1);
        return new Notification(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
                Boolean.parseBoolean(p[7]));
    }

    public String toFileString() {
        return String.join("|", notifId, recipientId, senderRole,
                issueId, assignId, message, createdAt, String.valueOf(read));
    }

    public String getNotifId()     { return notifId; }
    public String getRecipientId() { return recipientId; }
    public String getSenderRole()  { return senderRole; }
    public String getIssueId()     { return issueId; }
    public String getAssignId()    { return assignId; }
    public String getMessage()     { return message; }
    public String getCreatedAt()   { return createdAt; }
    public boolean isRead()        { return read; }

    public void setNotifId(String v)     { this.notifId     = v; }
    public void setRecipientId(String v) { this.recipientId = v; }
    public void setSenderRole(String v)  { this.senderRole  = v; }
    public void setIssueId(String v)     { this.issueId     = v; }
    public void setAssignId(String v)    { this.assignId    = v; }
    public void setMessage(String v)     { this.message     = v; }
    public void setCreatedAt(String v)   { this.createdAt   = v; }
    public void setRead(boolean v)       { this.read        = v; }
}
