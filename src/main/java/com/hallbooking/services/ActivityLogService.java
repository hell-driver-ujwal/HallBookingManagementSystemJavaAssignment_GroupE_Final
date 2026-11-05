package com.hallbooking.services;

import com.hallbooking.models.ActivityLog;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ActivityLogService — logs and retrieves system activity.
 * Called from AuthService, BookingService, IssueService, UserService, etc.
 */
public class ActivityLogService {

    // ── Log a new action ─────────────────────────────────────────────
    public static void log(String userId, String role, String action, String details) {
        try {
            String logId = FileManager.getNextId("LOG", FileManager.ACTIVITY_LOG);
            ActivityLog entry = new ActivityLog(
                    logId, userId, role, action, details, DateTimeUtils.now());
            FileManager.appendLine(FileManager.ACTIVITY_LOG, entry.toFileString());
        } catch (Exception ignored) {
            // Never let logging crash the app
        }
    }

    // ── Read all logs ─────────────────────────────────────────────────
    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.ACTIVITY_LOG)) {
            try { list.add(ActivityLog.fromString(line)); }
            catch (Exception ignored) {}
        }
        return list;
    }

    // ── Get recent N logs ─────────────────────────────────────────────
    public List<ActivityLog> getRecentLogs(int limit) {
        List<ActivityLog> all = getAllLogs();
        int from = Math.max(0, all.size() - limit);
        List<ActivityLog> recent = new ArrayList<>(all.subList(from, all.size()));
        java.util.Collections.reverse(recent);
        return recent;
    }

    // ── Get logs for a specific user ──────────────────────────────────
    public List<ActivityLog> getLogsByUser(String userId) {
        return getAllLogs().stream()
                .filter(l -> l.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    // ── Get logs by action type ───────────────────────────────────────
    public List<ActivityLog> getLogsByAction(String action) {
        return getAllLogs().stream()
                .filter(l -> l.getAction().equals(action))
                .collect(Collectors.toList());
    }

    // ── Common log shortcuts ──────────────────────────────────────────
    public static void logLogin(String userId, String role, String email) {
        log(userId, role, "LOGIN", "User logged in: " + email);
    }

    public static void logLogout(String userId, String role) {
        log(userId, role, "LOGOUT", "User logged out");
    }

    public static void logBookingCreated(String userId, String bookingId, String hallId) {
        log(userId, "CUSTOMER", "BOOKING_CREATED",
                "Booking " + bookingId + " for hall " + hallId);
    }

    public static void logBookingCancelled(String userId, String bookingId) {
        log(userId, "CUSTOMER", "BOOKING_CANCELLED", "Booking " + bookingId + " cancelled");
    }

    public static void logIssueReported(String userId, String issueId) {
        log(userId, "CUSTOMER", "ISSUE_REPORTED", "Issue " + issueId + " reported");
    }

    public static void logIssueAssigned(String userId, String issueId, String staffId) {
        log(userId, "MANAGER", "ISSUE_ASSIGNED",
                "Issue " + issueId + " assigned to " + staffId);
    }

    public static void logPasswordReset(String adminId, String targetUserId) {
        log(adminId, "ADMIN", "PASSWORD_RESET",
                "Password reset for user " + targetUserId);
    }

    public static void logStaffCreated(String adminId, String staffId, String name) {
        log(adminId, "ADMIN", "STAFF_CREATED",
                "Staff account created: " + name + " (" + staffId + ")");
    }

    public static void logUserBlocked(String adminId, String targetUserId) {
        log(adminId, "ADMIN", "USER_BLOCKED", "User " + targetUserId + " blocked");
    }

    public static void logUserUnblocked(String adminId, String targetUserId) {
        log(adminId, "ADMIN", "USER_UNBLOCKED", "User " + targetUserId + " unblocked");
    }

    public static void logMaintenanceScheduled(String staffId, String maintId, String hallId) {
        log(staffId, "SCHEDULER", "MAINTENANCE_SCHEDULED",
                "Maintenance " + maintId + " scheduled for hall " + hallId);
    }
}
