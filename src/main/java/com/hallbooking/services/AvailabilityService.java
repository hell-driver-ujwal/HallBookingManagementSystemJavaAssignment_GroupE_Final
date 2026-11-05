package com.hallbooking.services;

import com.hallbooking.models.HallAvailability;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * AvailabilityService — manages weekly operating schedules per hall.
 *
 * Each hall has ONE rule that defines which days and hours it is open.
 * The scheduler sets this once; BookingValidator enforces it on every booking.
 */
public class AvailabilityService {

    // ── Get all rules ──────────────────────────────────────────────
    public List<HallAvailability> getAllSlots() {
        List<HallAvailability> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.AVAILABILITY))
            list.add(HallAvailability.fromString(line));
        return list;
    }

    // ── Get rule for a specific hall (null if none set) ────────────
    public HallAvailability getRuleByHall(String hallId) {
        for (HallAvailability rule : getAllSlots())
            if (rule.getHallId().equals(hallId)) return rule;
        return null;
    }

    // ── Save (add or update) a rule for a hall ─────────────────────
    // If a rule already exists for this hall, it is replaced.
    public void saveRule(String hallId, String openDays, String openTime,
                         String closeTime) {
        String staffId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        HallAvailability existing = getRuleByHall(hallId);

        if (existing != null) {
            // Update existing rule
            existing.setOpenDays(openDays);
            existing.setOpenTime(openTime);
            existing.setCloseTime(closeTime);
            existing.setSetByStaffId(staffId);
            FileManager.updateLine(FileManager.AVAILABILITY, existing.getRuleId(),
                    existing.toFileString());
        } else {
            // Create new rule
            String ruleId = FileManager.getNextId("HR", FileManager.AVAILABILITY);
            HallAvailability rule = new HallAvailability(
                    ruleId, hallId, openDays, openTime, closeTime, staffId);
            FileManager.appendLine(FileManager.AVAILABILITY, rule.toFileString());
        }
    }

    // ── Delete a rule for a hall ───────────────────────────────────
    public void deleteRule(String ruleId) {
        FileManager.deleteLine(FileManager.AVAILABILITY, ruleId);
    }
}