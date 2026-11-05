package com.hallbooking.services;

import com.hallbooking.models.HallAvailability;
import com.hallbooking.models.Maintenance;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * MaintenanceService — Scheduler schedules and updates maintenance.
 *
 * Uses Maintenance.isResolved() (polymorphism via HallEvent) to filter
 * active maintenance records.
 */
public class MaintenanceService {

    public List<Maintenance> getAllMaintenance() {
        List<Maintenance> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.MAINTENANCE))
            list.add(Maintenance.fromString(line));
        return list;
    }

    public List<Maintenance> getMaintenanceByHall(String hallId) {
        List<Maintenance> list = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.MAINTENANCE, 1, hallId))
            list.add(Maintenance.fromString(line));
        return list;
    }

    /**
     * Returns SCHEDULED and IN_PROGRESS maintenance only.
     * Uses Maintenance.isResolved() — demonstrating polymorphism via HallEvent.
     */
    public List<Maintenance> getActiveMaintenance() {
        List<Maintenance> list = new ArrayList<>();
        for (Maintenance m : getAllMaintenance())
            if (!m.isResolved()) list.add(m);
        return list;
    }

    public Maintenance getMaintenanceById(String maintId) {
        String line = FileManager.findById(FileManager.MAINTENANCE, maintId);
        return line != null ? Maintenance.fromString(line) : null;
    }

    public String addMaintenance(String hallId, String startDatetime,
                                  String endDatetime, String remarks) {
        // Validate against this hall's actual operating hours (not hardcoded 08-18)
        com.hallbooking.models.HallAvailability rule =
                new AvailabilityService().getRuleByHall(hallId);
        if (rule != null) {
            java.time.LocalTime openTime  = java.time.LocalTime.parse(rule.getOpenTime());
            java.time.LocalTime closeTime = java.time.LocalTime.parse(rule.getCloseTime());
            java.time.LocalDateTime startDt = DateTimeUtils.parse(startDatetime);
            java.time.LocalDateTime endDt   = DateTimeUtils.parse(endDatetime);
            if (startDt.toLocalTime().isBefore(openTime)
                    || endDt.toLocalTime().isAfter(closeTime)
                    || !endDt.isAfter(startDt)
                    || !startDt.toLocalDate().equals(endDt.toLocalDate())) {
                return "Maintenance must be within this hall's operating hours ("
                        + rule.getOpenTime() + " – " + rule.getCloseTime()
                        + ") and on the same day.";
            }
        } else {
            // No schedule set — fall back to the default operating hours check
            if (!DateTimeUtils.isWithinOperatingHours(startDatetime, endDatetime))
                return "Maintenance must be scheduled within operating hours (08:00 – 18:00).";
        }
        if (remarks.trim().isEmpty())
            return "Please describe what maintenance will be performed.";

        String staffId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        String maintId = FileManager.getNextId("M", FileManager.MAINTENANCE);
        Maintenance m  = new Maintenance(maintId, hallId, startDatetime,
                endDatetime, remarks, "SCHEDULED", staffId);
        FileManager.appendLine(FileManager.MAINTENANCE, m.toFileString());
        ActivityLogService.logMaintenanceScheduled(staffId, maintId, hallId);
        return null;
    }

    public boolean updateStatus(String maintId, String newStatus) {
        Maintenance m = getMaintenanceById(maintId);
        if (m == null) return false;
        m.setStatus(newStatus);
        return FileManager.updateLine(FileManager.MAINTENANCE, maintId, m.toFileString());
    }

    /**
     * Checks if a proposed maintenance window overlaps with any CONFIRMED booking.
     * Returns a warning string if conflict found, null if clear.
     */
    public String checkBookingConflict(String hallId, String startDatetime, String endDatetime) {
        BookingService bookingService = new BookingService();
        for (com.hallbooking.models.Booking b : bookingService.getConfirmedByHall(hallId)) {
            if (com.hallbooking.utils.DateTimeUtils.overlaps(
                    startDatetime, endDatetime, b.getEventStart(), b.getEventEnd())) {
                return "⚠ Conflicts with confirmed booking " + b.getBookingId()
                        + "\n(" + com.hallbooking.utils.DateTimeUtils.toDisplayFormat(b.getEventStart())
                        + " – " + com.hallbooking.utils.DateTimeUtils.toDisplayFormat(b.getEventEnd()) + ")";
            }
        }
        return null;
    }

    public String deleteMaintenance(String maintId) {
        Maintenance m = getMaintenanceById(maintId);
        if (m == null) return "Maintenance record not found.";
        if (!"SCHEDULED".equals(m.getStatus()))
            return "Only SCHEDULED maintenance can be deleted.";
        FileManager.deleteLine(FileManager.MAINTENANCE, maintId);
        return null;
    }
}
