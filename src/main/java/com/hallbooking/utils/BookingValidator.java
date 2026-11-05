package com.hallbooking.utils;

import com.hallbooking.models.Booking;
import com.hallbooking.models.HallAvailability;
import com.hallbooking.models.Maintenance;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * BookingValidator — runs 4 checks before any booking is confirmed.
 *
 * Check 1: Hall schedule rule — is the day and time within the hall's weekly operating hours?
 * Check 2: Same calendar day — start and end must be on the same day
 * Check 3: Maintenance overlap — uses Maintenance.isResolved() (HallEvent polymorphism)
 * Check 4: Booking conflict
 */
public class BookingValidator {

    public static String validate(String reqStart, String reqEnd,
                                  HallAvailability rule,
                                  List<Maintenance> maintenanceList,
                                  List<Booking> bookingList) {
        String err;
        if ((err = checkScheduleRule(reqStart, reqEnd, rule))                   != null) return err;
        if ((err = checkSameDay(reqStart, reqEnd))                              != null) return err;
        if ((err = checkMaintenanceConflict(reqStart, reqEnd, maintenanceList)) != null) return err;
        if ((err = checkBookingConflict(reqStart, reqEnd, bookingList))         != null) return err;
        return null;
    }

    // ── Check 1: hall's weekly schedule rule ──────────────────────
    public static String checkScheduleRule(String start, String end, HallAvailability rule) {
        if (rule == null) {
            return "This hall has no operating schedule set. "
                    + "Please contact the scheduler to configure availability.";
        }

        LocalDateTime startDt = DateTimeUtils.parse(start);
        LocalDateTime endDt   = DateTimeUtils.parse(end);

        String dayAbbr = startDt.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        List<String> openDays = List.of(rule.getOpenDays().split(","));
        if (!openDays.contains(dayAbbr)) {
            return "This hall is not open on "
                    + startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + "s.\nOpen days: " + rule.getOpenDays().replace(",", ", ");
        }

        LocalTime openTime  = LocalTime.parse(rule.getOpenTime());
        LocalTime closeTime = LocalTime.parse(rule.getCloseTime());
        LocalTime reqStart  = startDt.toLocalTime();
        LocalTime reqEnd    = endDt.toLocalTime();

        // Check end-before-start FIRST so the error message is correct
        if (!reqEnd.isAfter(reqStart)) {
            return "End time must be after start time.";
        }

        if (reqStart.isBefore(openTime) || reqEnd.isAfter(closeTime)) {
            return "This hall is only open "
                    + rule.getOpenTime() + " – " + rule.getCloseTime() + ".\n"
                    + "Your booking (" + reqStart + " – " + reqEnd + ") is outside these hours.";
        }

        return null;
    }

    // ── Check 2: same calendar day ────────────────────────────────
    public static String checkSameDay(String start, String end) {
        LocalDateTime s = DateTimeUtils.parse(start);
        LocalDateTime e = DateTimeUtils.parse(end);
        if (!s.toLocalDate().equals(e.toLocalDate())) {
            return "Bookings must start and end on the same day.";
        }
        return null;
    }

    // ── Check 3: no overlap with active maintenance ───────────────
    // Uses Maintenance.isResolved() — polymorphism via HallEvent
    public static String checkMaintenanceConflict(String reqStart, String reqEnd,
                                                  List<Maintenance> maintenanceList) {
        for (Maintenance m : maintenanceList) {
            if (m.isResolved()) continue; // skip DONE maintenance (polymorphic call)
            if (DateTimeUtils.overlaps(reqStart, reqEnd,
                    m.getStartDatetime(), m.getEndDatetime())) {
                return "The hall is under maintenance during that time:\n"
                        + "  \"" + m.getRemarks() + "\"\n"
                        + "  (" + DateTimeUtils.toDisplayFormat(m.getStartDatetime())
                        + " – " + DateTimeUtils.toDisplayFormat(m.getEndDatetime()) + ")";
            }
        }
        return null;
    }

    // ── Check 4: no overlap with confirmed bookings ───────────────
    public static String checkBookingConflict(String reqStart, String reqEnd,
                                              List<Booking> bookingList) {
        for (Booking b : bookingList) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            if (DateTimeUtils.overlaps(reqStart, reqEnd,
                    b.getEventStart(), b.getEventEnd())) {
                return "That time slot is already booked.\n"
                        + "  Booking " + b.getBookingId() + ": "
                        + DateTimeUtils.toDisplayFormat(b.getEventStart())
                        + " – " + DateTimeUtils.toDisplayFormat(b.getEventEnd());
            }
        }
        return null;
    }

    // ── Cancellation: at least 3 days (72 hours) before event ────────────────
    public static String checkCancellationWindow(String eventStart) {
        long hoursUntil = java.time.temporal.ChronoUnit.HOURS.between(
                java.time.LocalDateTime.now(),
                DateTimeUtils.parse(eventStart));
        if (hoursUntil < 72) {
            long daysUntil = hoursUntil / 24;
            return "Cancellation is only allowed at least 3 days (72 hours) before the event.\n"
                    + "Your event is in approximately " + daysUntil + " day(s) ("
                    + hoursUntil + " hours).";
        }
        return null;
    }
}
