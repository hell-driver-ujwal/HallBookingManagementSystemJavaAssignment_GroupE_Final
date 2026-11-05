package com.hallbooking.services;

import com.hallbooking.models.Booking;
import com.hallbooking.models.HallAvailability;
import com.hallbooking.models.Maintenance;
import com.hallbooking.utils.BookingValidator;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BookingService — Customer books and cancels. Admin/Manager views all.
 *
 * Functions:
 *   createBooking(...)                    → Customer books a hall (runs all 4 validation checks)
 *   cancelBooking(bookingId)              → Customer cancels (enforces 3-day rule)
 *   getBookingsByCustomer(customerId)     → Customer's own booking history
 *   getUpcomingByCustomer(customerId)     → Upcoming confirmed bookings only
 *   getPastByCustomer(customerId)         → Past bookings
 *   getAllBookings()                       → Admin/Manager: all bookings
 *   getBookingsByHall(hallId)             → Filter by hall
 *   getConfirmedByHall(hallId)            → CONFIRMED bookings for conflict check
 *   getBookingById(bookingId)             → Single lookup
 *   searchBookings(keyword)               → Filter by bookingId/hallId/customerId
 *   getTotalRevenue()                     → Manager: sum of all confirmed amounts
 *   getRevenueByMonth(year, month)        → Manager: monthly revenue
 */
public class BookingService {

    private final MaintenanceService maintenanceService = new MaintenanceService();

    // ── Create a new booking ───────────────────────────────────────
    // Returns the new bookingId on success, or an error message prefixed with "ERR:" on failure.
    // Callers check: result.startsWith("ERR:") for failure, otherwise it's the bookingId.
    public String createBooking(String customerId, String hallId, String availId,
                                String eventStart, String eventEnd) {
        // Gather existing data for validation
        List<Maintenance> maintenances = maintenanceService.getMaintenanceByHall(hallId);
        List<Booking>     existing     = getConfirmedByHall(hallId);
        HallAvailability  rule         = new AvailabilityService().getRuleByHall(hallId);

        // Run all 4 validation checks
        String error = BookingValidator.validate(eventStart, eventEnd, rule, maintenances, existing);
        if (error != null) return "ERR:" + error;

        // Calculate financials
        double hours  = DateTimeUtils.hoursBetween(eventStart, eventEnd);
        double rate   = new HallService().getHallById(hallId).getRatePerHour();
        double amount = hours * rate;

        String bookingId = FileManager.getNextId("B", FileManager.BOOKINGS);
        Booking booking  = new Booking(bookingId, customerId, hallId,
                availId != null ? availId : "NULL",
                eventStart, eventEnd, hours, amount,
                "CONFIRMED", DateTimeUtils.now(), "NULL");
        FileManager.appendLine(FileManager.BOOKINGS, booking.toFileString());
        ActivityLogService.logBookingCreated(customerId, bookingId, hallId);
        return bookingId;
    }

    // ── Cancel a booking ───────────────────────────────────────────
    public String cancelBooking(String bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking == null) return "Booking not found.";
        if ("CANCELLED".equals(booking.getStatus())) return "Booking is already cancelled.";

        // Enforce 3-day cancellation window
        String error = BookingValidator.checkCancellationWindow(booking.getEventStart());
        if (error != null) return error;

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(DateTimeUtils.now());
        FileManager.updateLine(FileManager.BOOKINGS, bookingId, booking.toFileString());
        ActivityLogService.logBookingCancelled(booking.getCustomerId(), bookingId);
        return null;
    }

    // ── Customer views ─────────────────────────────────────────────
    public List<Booking> getBookingsByCustomer(String customerId) {
        List<Booking> list = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.BOOKINGS, 1, customerId))
            list.add(Booking.fromString(line));
        return list;
    }

    public List<Booking> getUpcomingByCustomer(String customerId) {
        List<Booking> list = new ArrayList<>();
        for (Booking b : getBookingsByCustomer(customerId))
            if ("CONFIRMED".equals(b.getStatus()) && !DateTimeUtils.isInPast(b.getEventStart()))
                list.add(b);
        return list;
    }

    public List<Booking> getPastByCustomer(String customerId) {
        List<Booking> list = new ArrayList<>();
        for (Booking b : getBookingsByCustomer(customerId))
            if (DateTimeUtils.isInPast(b.getEventEnd()))
                list.add(b);
        return list;
    }

    // ── Admin / Manager views ──────────────────────────────────────
    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.BOOKINGS))
            list.add(Booking.fromString(line));
        return list;
    }

    public List<Booking> getBookingsByHall(String hallId) {
        List<Booking> list = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.BOOKINGS, 2, hallId))
            list.add(Booking.fromString(line));
        return list;
    }

    public List<Booking> getConfirmedByHall(String hallId) {
        List<Booking> list = new ArrayList<>();
        for (Booking b : getBookingsByHall(hallId))
            if ("CONFIRMED".equals(b.getStatus())) list.add(b);
        return list;
    }

    public Booking getBookingById(String bookingId) {
        String line = FileManager.findById(FileManager.BOOKINGS, bookingId);
        return line != null ? Booking.fromString(line) : null;
    }

    public List<Booking> searchBookings(String keyword) {
        String kw = keyword.toLowerCase();
        List<Booking> result = new ArrayList<>();
        for (Booking b : getAllBookings())
            if (b.getBookingId().toLowerCase().contains(kw)
                    || b.getCustomerId().toLowerCase().contains(kw)
                    || b.getHallId().toLowerCase().contains(kw)) result.add(b);
        return result;
    }

    // ── Revenue calculations (Manager) ────────────────────────────
    public double getTotalRevenue() {
        double total = 0;
        for (Booking b : getAllBookings())
            if ("CONFIRMED".equals(b.getStatus())) total += b.getTotalAmount();
        return total;
    }

    public double getRevenueByMonth(int year, int month) {
        double total = 0;
        String prefix = String.format("%04d-%02d", year, month);
        for (Booking b : getAllBookings())
            if ("CONFIRMED".equals(b.getStatus()) && b.getEventStart().startsWith(prefix))
                total += b.getTotalAmount();
        return total;
    }

    public double getRevenueByYear(int year) {
        double total = 0;
        String prefix = String.valueOf(year);
        for (Booking b : getAllBookings())
            if ("CONFIRMED".equals(b.getStatus()) && b.getEventStart().startsWith(prefix))
                total += b.getTotalAmount();
        return total;
    }

    // ── Revenue for a specific week (Mon–Sun) ──────────────────────
    public double getRevenueByWeek(java.time.LocalDate weekStart) {
        java.time.LocalDate weekEnd = weekStart.plusDays(6);
        double total = 0;
        for (Booking b : getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(b.getEventStart().substring(0, 10));
                if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) total += b.getTotalAmount();
            } catch (Exception ignored) {}
        }
        return total;
    }

    // ── All confirmed bookings within a week ───────────────────────
    public List<Booking> getBookingsForWeek(java.time.LocalDate weekStart) {
        java.time.LocalDate weekEnd = weekStart.plusDays(6);
        List<Booking> list = new ArrayList<>();
        for (Booking b : getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(b.getEventStart().substring(0, 10));
                if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) list.add(b);
            } catch (Exception ignored) {}
        }
        return list;
    }
}