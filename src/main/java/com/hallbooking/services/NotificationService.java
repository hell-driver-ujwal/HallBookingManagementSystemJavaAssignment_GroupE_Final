package com.hallbooking.services;

import com.hallbooking.models.Notification;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and reads in-app notifications stored in notifications.txt.
 *
 * Two notification paths:
 *  1. Manager assigns an issue → notify the scheduler (recipientId = schedulerId)
 *  2. Scheduler marks done     → notify the manager   (recipientId = managerId)
 */
public class NotificationService {

    // ── Create ────────────────────────────────────────────────────────

    /**
     * Called when a manager assigns an issue to a scheduler.
     * Scheduler is the recipient.
     */
    public void notifySchedulerAssigned(String schedulerId, String managerId,
                                        String issueId, String assignId) {
        String msg = "You have been assigned issue " + issueId + " by manager " + managerId + ".";
        createNotification(schedulerId, "MANAGER", issueId, assignId, msg);
    }

    /**
     * Called when a scheduler marks an issue as done.
     * Manager is the recipient.
     */
    public void notifyManagerResolved(String managerId, String schedulerId,
                                      String issueId, String assignId,
                                      String resolutionNote) {
        String msg = "Scheduler " + schedulerId + " has resolved issue " + issueId
                + ". Note: " + resolutionNote;
        createNotification(managerId, "SCHEDULER", issueId, assignId, msg);
    }

    private void createNotification(String recipientId, String senderRole,
                                    String issueId, String assignId, String message) {
        String id = FileManager.getNextId("N", FileManager.NOTIFICATIONS);
        Notification n = new Notification(id, recipientId, senderRole,
                issueId, assignId, message, DateTimeUtils.now(), false);
        FileManager.appendLine(FileManager.NOTIFICATIONS, n.toFileString());
    }

    // ── Read ──────────────────────────────────────────────────────────

    public List<Notification> getUnreadForRecipient(String recipientId) {
        List<Notification> result = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.NOTIFICATIONS, 1, recipientId)) {
            try {
                Notification n = Notification.fromString(line);
                if (!n.isRead()) result.add(n);
            } catch (Exception ignored) {}
        }
        return result;
    }

    public List<Notification> getAllForRecipient(String recipientId) {
        List<Notification> result = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.NOTIFICATIONS, 1, recipientId)) {
            try { result.add(Notification.fromString(line)); }
            catch (Exception ignored) {}
        }
        return result;
    }

    /** Marks a single notification as read. */
    public void markRead(String notifId) {
        String line = FileManager.findById(FileManager.NOTIFICATIONS, notifId);
        if (line == null) return;
        Notification n = Notification.fromString(line);
        n.setRead(true);
        FileManager.updateLine(FileManager.NOTIFICATIONS, notifId, n.toFileString());
    }

    /** Marks all unread notifications for a recipient as read. */
    public void markAllRead(String recipientId) {
        getUnreadForRecipient(recipientId).forEach(n -> markRead(n.getNotifId()));
    }

    public long countUnread(String recipientId) {
        return getUnreadForRecipient(recipientId).size();
    }
}
