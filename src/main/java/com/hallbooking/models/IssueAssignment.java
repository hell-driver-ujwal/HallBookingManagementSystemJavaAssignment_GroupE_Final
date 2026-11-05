package com.hallbooking.models;

/**
 * Represents a manager's assignment of an issue to a scheduler.
 *
 * File format (issue_assignments.txt):
 *   assignId | issueId | assignedToStaffId | assignedByManagerId | assignedAt
 *            | customerNote | schedulerNote | completedAt
 *
 * customerNote  — manager's message visible to the customer
 * schedulerNote — manager's private instructions to the scheduler
 *                 (on completion, overwritten with the scheduler's resolution note to manager)
 * completedAt   — "NULL" if not yet done
 *
 * Backward-compat: old 7-field records map to the new 8-field layout;
 * the old single "managerResponse" becomes customerNote, schedulerNote defaults to "".
 */
public class IssueAssignment {

    private String assignId;
    private String issueId;
    private String assignedToStaffId;
    private String assignedByManagerId;
    private String assignedAt;
    private String customerNote;    // visible to customer
    private String schedulerNote;   // manager->scheduler instructions; later overwritten by scheduler's resolution note to manager
    private String completedAt;     // "NULL" if pending

    public IssueAssignment() {}

    public IssueAssignment(String assignId, String issueId, String assignedToStaffId,
                           String assignedByManagerId, String assignedAt,
                           String customerNote, String schedulerNote, String completedAt) {
        this.assignId            = assignId;
        this.issueId             = issueId;
        this.assignedToStaffId   = assignedToStaffId;
        this.assignedByManagerId = assignedByManagerId;
        this.assignedAt          = assignedAt;
        this.customerNote        = customerNote  != null ? customerNote  : "";
        this.schedulerNote       = schedulerNote != null ? schedulerNote : "";
        this.completedAt         = completedAt   != null ? completedAt   : "NULL";
    }

    public static IssueAssignment fromString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length >= 8) {
            return new IssueAssignment(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);
        } else if (p.length == 7) {
            // Old 7-field: managerResponse -> customerNote, schedulerNote = ""
            return new IssueAssignment(p[0], p[1], p[2], p[3], p[4], p[5], "", p[6]);
        } else {
            throw new IllegalArgumentException("Malformed IssueAssignment: " + line);
        }
    }

    public String toFileString() {
        return String.join("|",
                assignId, issueId, assignedToStaffId, assignedByManagerId, assignedAt,
                customerNote  != null ? customerNote  : "",
                schedulerNote != null ? schedulerNote : "",
                completedAt   != null ? completedAt   : "NULL");
    }

    // Getters
    public String getAssignId()            { return assignId; }
    public String getIssueId()             { return issueId; }
    public String getAssignedToStaffId()   { return assignedToStaffId; }
    public String getAssignedByManagerId() { return assignedByManagerId; }
    public String getAssignedAt()          { return assignedAt; }
    public String getCustomerNote()        { return customerNote  != null ? customerNote  : ""; }
    public String getSchedulerNote()       { return schedulerNote != null ? schedulerNote : ""; }
    public String getCompletedAt()         { return completedAt   != null ? completedAt   : "NULL"; }
    /** Legacy accessor — returns customerNote. */
    public String getManagerResponse()     { return getCustomerNote(); }

    // Setters
    public void setAssignId(String v)            { this.assignId            = v; }
    public void setIssueId(String v)             { this.issueId             = v; }
    public void setAssignedToStaffId(String v)   { this.assignedToStaffId   = v; }
    public void setAssignedByManagerId(String v) { this.assignedByManagerId = v; }
    public void setAssignedAt(String v)          { this.assignedAt          = v; }
    public void setCustomerNote(String v)        { this.customerNote        = v; }
    public void setSchedulerNote(String v)       { this.schedulerNote       = v; }
    public void setCompletedAt(String v)         { this.completedAt         = v; }
    /** Legacy setter — sets customerNote. */
    public void setManagerResponse(String v)     { this.customerNote        = v; }
}
