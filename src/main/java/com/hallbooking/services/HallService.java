package com.hallbooking.services;

import com.hallbooking.models.Hall;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * HallService — Scheduler manages halls. Everyone reads halls.
 *
 * Functions:
 *   getAllHalls()            → all halls (any role)
 *   getActiveHalls()         → active halls only (customer browse)
 *   getHallById(id)          → single hall lookup
 *   addHall(hall)            → Scheduler adds a hall
 *   updateHall(hall)         → Scheduler edits a hall
 *   deleteHall(hallId)       → Scheduler removes a hall (soft-delete)
 *   searchHalls(keyword)     → filter by name or type
 *   getHallsByType(type)     → filter by AUDITORIUM / BANQUET_HALL / MEETING_ROOM
 */
public class HallService {

    public List<Hall> getAllHalls() {
        List<Hall> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.HALLS))
            list.add(Hall.fromString(line));
        return list;
    }

    public List<Hall> getActiveHalls() {
        List<Hall> list = new ArrayList<>();
        for (Hall h : getAllHalls())
            if (h.isActive()) list.add(h);
        return list;
    }

    public Hall getHallById(String hallId) {
        String line = FileManager.findById(FileManager.HALLS, hallId);
        return line != null ? Hall.fromString(line) : null;
    }

    public String addHall(String hallName, String hallType, int capacity,
                          double ratePerHour, String description) {
        if (hallName.trim().isEmpty())    return "Hall name is required.";
        if (hallType.trim().isEmpty())    return "Hall type is required.";
        if (capacity <= 0)                return "Capacity must be greater than 0.";
        if (ratePerHour <= 0)             return "Rate must be greater than 0.";

        String staffId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        String hallId  = FileManager.getNextId("H", FileManager.HALLS);
        Hall hall = new Hall(hallId, hallName, hallType, capacity,
                ratePerHour, description, true, staffId);
        FileManager.appendLine(FileManager.HALLS, hall.toFileString());
        return null;
    }

    public boolean updateHall(Hall hall) {
        return FileManager.updateLine(FileManager.HALLS, hall.getHallId(), hall.toFileString());
    }

    // Soft-delete: set isActive=false instead of removing the record
    public boolean deleteHall(String hallId) {
        Hall hall = getHallById(hallId);
        if (hall == null) return false;
        hall.setActive(false);
        return FileManager.updateLine(FileManager.HALLS, hallId, hall.toFileString());
    }

    public List<Hall> searchHalls(String keyword) {
        String kw = keyword.toLowerCase();
        List<Hall> result = new ArrayList<>();
        for (Hall h : getAllHalls())
            if (h.getHallName().toLowerCase().contains(kw)
                    || h.getHallType().toLowerCase().contains(kw)) result.add(h);
        return result;
    }

    public List<Hall> getHallsByType(String type) {
        List<Hall> result = new ArrayList<>();
        for (Hall h : getActiveHalls())
            if (h.getHallType().equalsIgnoreCase(type)) result.add(h);
        return result;
    }
}
