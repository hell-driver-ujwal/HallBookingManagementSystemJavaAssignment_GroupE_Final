package com.hallbooking.models;

/**
 * HallAvailability — stores the weekly operating schedule for a hall.
 *
 * Format in availability.txt:
 *   ruleId|hallId|openDays|openTime|closeTime|setByStaffId
 *
 * openDays: comma-separated day names: MON,TUE,WED,THU,FRI,SAT,SUN
 * openTime / closeTime: HH:mm  e.g. 08:00 / 18:00
 *
 * One row per hall. The scheduler sets this once; it applies every week.
 */
public class HallAvailability {

    private String ruleId;
    private String hallId;
    private String openDays;    // e.g. "MON,TUE,WED,THU,FRI"
    private String openTime;    // e.g. "08:00"
    private String closeTime;   // e.g. "18:00"
    private String setByStaffId;

    public HallAvailability() {}

    public HallAvailability(String ruleId, String hallId, String openDays,
                            String openTime, String closeTime, String setByStaffId) {
        this.ruleId       = ruleId;
        this.hallId       = hallId;
        this.openDays     = openDays;
        this.openTime     = openTime;
        this.closeTime    = closeTime;
        this.setByStaffId = setByStaffId;
    }

    public static HallAvailability fromString(String line) {
        String[] p = line.split("\\|");
        return new HallAvailability(p[0], p[1], p[2], p[3], p[4], p[5]);
    }

    public String toFileString() {
        return String.join("|", ruleId, hallId, openDays, openTime, closeTime, setByStaffId);
    }

    // ── Getters ───────────────────────────────────────────────────
    public String getRuleId()       { return ruleId; }
    public String getHallId()       { return hallId; }
    public String getOpenDays()     { return openDays; }
    public String getOpenTime()     { return openTime; }
    public String getCloseTime()    { return closeTime; }
    public String getSetByStaffId() { return setByStaffId; }

    // ── Setters ───────────────────────────────────────────────────
    public void setRuleId(String ruleId)             { this.ruleId = ruleId; }
    public void setHallId(String hallId)             { this.hallId = hallId; }
    public void setOpenDays(String openDays)         { this.openDays = openDays; }
    public void setOpenTime(String openTime)         { this.openTime = openTime; }
    public void setCloseTime(String closeTime)       { this.closeTime = closeTime; }
    public void setSetByStaffId(String setByStaffId) { this.setByStaffId = setByStaffId; }

    /** Human-readable summary e.g. "Mon–Fri  08:00 – 18:00" */
    public String toDisplayString() {
        return openDays.replace(",", ", ") + "   " + openTime + " – " + closeTime;
    }
}