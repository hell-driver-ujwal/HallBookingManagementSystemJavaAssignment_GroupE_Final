package com.hallbooking.models;

public class Hall {
    private String hallId;
    private String hallName;
    private String hallType; // AUDITORIUM, BANQUET_HALL, MEETING_ROOM
    private int capacity;
    private double ratePerHour;
    private String description;
    private boolean isActive;
    private String addedByStaffId;

    public Hall() {}

    public Hall(String hallId, String hallName, String hallType, int capacity,
                double ratePerHour, String description, boolean isActive, String addedByStaffId) {
        this.hallId = hallId;
        this.hallName = hallName;
        this.hallType = hallType;
        this.capacity = capacity;
        this.ratePerHour = ratePerHour;
        this.description = description;
        this.isActive = isActive;
        this.addedByStaffId = addedByStaffId;
    }

    public static Hall fromString(String line) {
        String[] p = line.split("\\|");
        return new Hall(p[0], p[1], p[2], Integer.parseInt(p[3]),
                Double.parseDouble(p[4]), p[5], Boolean.parseBoolean(p[6]), p[7]);
    }

    public String toFileString() {
        return String.join("|", hallId, hallName, hallType, String.valueOf(capacity),
                String.format("%.2f", ratePerHour), description,
                String.valueOf(isActive), addedByStaffId);
    }

    public String getHallId()        { return hallId; }
    public String getHallName()      { return hallName; }
    public String getHallType()      { return hallType; }
    public int getCapacity()         { return capacity; }
    public double getRatePerHour()   { return ratePerHour; }
    public String getDescription()   { return description; }
    public boolean isActive()        { return isActive; }
    public String getAddedByStaffId(){ return addedByStaffId; }

    public void setHallId(String hallId)             { this.hallId = hallId; }
    public void setHallName(String hallName)         { this.hallName = hallName; }
    public void setHallType(String hallType)         { this.hallType = hallType; }
    public void setCapacity(int capacity)            { this.capacity = capacity; }
    public void setRatePerHour(double ratePerHour)   { this.ratePerHour = ratePerHour; }
    public void setDescription(String description)   { this.description = description; }
    public void setActive(boolean active)            { this.isActive = active; }
    public void setAddedByStaffId(String id)         { this.addedByStaffId = id; }

    @Override
    public String toString() { return hallName + " (" + hallType + ")"; }
}
