package com.hallbooking.models;

/**
 * Staff — a staff member (Scheduler, Admin, or Manager).
 * Extends Person, inheriting fullName.
 * Phone for staff is stored in the linked User record, not here.
 */
public class Staff extends Person {

    private String staffId;
    private String userId;
    // fullName is inherited from Person; phone is in User for staff
    private String department;
    private String hireDate;
    private boolean isActive;
    private String createdByAdminId;

    public Staff() {}

    public Staff(String staffId, String userId, String fullName, String department,
                 String hireDate, boolean isActive, String createdByAdminId) {
        super(fullName, null); // staff phone lives in the User record
        this.staffId          = staffId;
        this.userId           = userId;
        this.department       = department;
        this.hireDate         = hireDate;
        this.isActive         = isActive;
        this.createdByAdminId = createdByAdminId;
    }

    public static Staff fromString(String line) {
        String[] p = line.split("\\|");
        return new Staff(p[0], p[1], p[2], p[3], p[4],
                Boolean.parseBoolean(p[5]), p[6]);
    }

    @Override
    public String toFileString() {
        return String.join("|", staffId, userId, fullName, department, hireDate,
                String.valueOf(isActive), createdByAdminId);
    }

    // Getters
    public String  getStaffId()          { return staffId; }
    public String  getUserId()           { return userId; }
    // getFullName() inherited from Person
    public String  getDepartment()       { return department; }
    public String  getHireDate()         { return hireDate; }
    public boolean isActive()            { return isActive; }
    public String  getCreatedByAdminId() { return createdByAdminId; }

    // Setters
    public void setStaffId(String staffId)                   { this.staffId          = staffId; }
    public void setUserId(String userId)                     { this.userId           = userId; }
    // setFullName() inherited from Person
    public void setDepartment(String department)             { this.department       = department; }
    public void setHireDate(String hireDate)                 { this.hireDate         = hireDate; }
    public void setActive(boolean active)                    { this.isActive         = active; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }

    @Override
    public String toString() {
        return "Staff{" + staffId + ", " + fullName + ", " + department + "}";
    }
}
