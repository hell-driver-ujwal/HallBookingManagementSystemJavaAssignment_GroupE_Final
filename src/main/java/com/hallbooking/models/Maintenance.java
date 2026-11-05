package com.hallbooking.models;

/**
 * Maintenance — a scheduled maintenance operation on a hall.
 * Extends HallEvent, inheriting hallId and status.
 *
 * Status values: SCHEDULED, IN_PROGRESS, DONE
 * isResolved() returns true only when the maintenance is DONE.
 */
public class Maintenance extends HallEvent {

    private String maintId;
    // hallId and status inherited from HallEvent
    private String startDatetime;
    private String endDatetime;
    private String remarks;
    private String scheduledByStaffId;

    public Maintenance() {}

    public Maintenance(String maintId, String hallId, String startDatetime,
                       String endDatetime, String remarks, String status,
                       String scheduledByStaffId) {
        super(hallId, status);
        this.maintId            = maintId;
        this.startDatetime      = startDatetime;
        this.endDatetime        = endDatetime;
        this.remarks            = remarks;
        this.scheduledByStaffId = scheduledByStaffId;
    }

    public static Maintenance fromString(String line) {
        String[] p = line.split("\\|");
        return new Maintenance(p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
    }

    @Override
    public String toFileString() {
        return String.join("|", maintId, hallId, startDatetime, endDatetime,
                remarks, status, scheduledByStaffId);
    }

    /**
     * Maintenance is resolved only when it is DONE.
     * SCHEDULED and IN_PROGRESS are both still active.
     */
    @Override
    public boolean isResolved() {
        return "DONE".equals(status);
    }

    // Getters
    public String getMaintId()            { return maintId; }
    // getHallId() and getStatus() inherited from HallEvent
    public String getStartDatetime()      { return startDatetime; }
    public String getEndDatetime()        { return endDatetime; }
    public String getRemarks()            { return remarks; }
    public String getScheduledByStaffId() { return scheduledByStaffId; }

    // Setters
    public void setMaintId(String maintId)                       { this.maintId            = maintId; }
    // setHallId() and setStatus() inherited from HallEvent
    public void setStartDatetime(String startDatetime)           { this.startDatetime      = startDatetime; }
    public void setEndDatetime(String endDatetime)               { this.endDatetime        = endDatetime; }
    public void setRemarks(String remarks)                       { this.remarks            = remarks; }
    public void setScheduledByStaffId(String scheduledByStaffId) { this.scheduledByStaffId = scheduledByStaffId; }

    @Override
    public String toString() {
        return "Maintenance{" + maintId + ", hall=" + hallId + ", " + status + "}";
    }
}
