package com.hallbooking.services;

import com.hallbooking.models.Issue;
import com.hallbooking.models.IssueAssignment;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IssueService {

    private final NotificationService notifService = new NotificationService();

    // ── Customer reports a new issue ───────────────────────────────
    public String reportIssue(String customerId, String bookingId,
                               String hallId, String title,
                               String description, String priority) {
        if (title.trim().isEmpty())       return "Issue title is required.";
        if (description.trim().isEmpty()) return "Please describe the issue.";
        if (priority == null || priority.trim().isEmpty()) priority = "MEDIUM";

        String issueId = FileManager.getNextId("I", FileManager.ISSUES);
        Issue issue = new Issue(issueId, customerId, bookingId, hallId,
                title, description, DateTimeUtils.now(), "OPEN", priority);
        FileManager.appendLine(FileManager.ISSUES, issue.toFileString());
        ActivityLogService.logIssueReported(customerId, issueId);
        return null;
    }

    public String reportIssue(String customerId, String bookingId,
                               String hallId, String title, String description) {
        return reportIssue(customerId, bookingId, hallId, title, description, "MEDIUM");
    }

    // ── Reads ──────────────────────────────────────────────────────
    public List<Issue> getAllIssues() {
        List<Issue> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.ISSUES)) {
            try { list.add(Issue.fromString(line)); }
            catch (Exception ignored) {}
        }
        return list;
    }

    public List<Issue> getIssuesByCustomer(String customerId) {
        List<Issue> list = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.ISSUES, 1, customerId)) {
            try { list.add(Issue.fromString(line)); }
            catch (Exception ignored) {}
        }
        return list;
    }

    public List<Issue> getOpenIssues() {
        return getAllIssues().stream()
                .filter(i -> !i.isResolved())
                .sorted((a, b) -> Integer.compare(a.getPriorityOrder(), b.getPriorityOrder()))
                .collect(Collectors.toList());
    }

    public List<Issue> getOverdueIssues() {
        return getAllIssues().stream().filter(Issue::isOverdue).collect(Collectors.toList());
    }

    public List<Issue> getHighPriorityIssues() {
        return getAllIssues().stream()
                .filter(i -> !i.isResolved() && "HIGH".equals(i.getPriority()))
                .collect(Collectors.toList());
    }

    public Issue getIssueById(String issueId) {
        String line = FileManager.findById(FileManager.ISSUES, issueId);
        return line != null ? Issue.fromString(line) : null;
    }

    // ── Manager updates issue status ───────────────────────────────
    public boolean updateIssueStatus(String issueId, String newStatus) {
        Issue issue = getIssueById(issueId);
        if (issue == null) return false;
        issue.setStatus(newStatus);
        return FileManager.updateLine(FileManager.ISSUES, issueId, issue.toFileString());
    }

    public boolean updateIssuePriority(String issueId, String newPriority) {
        Issue issue = getIssueById(issueId);
        if (issue == null) return false;
        issue.setPriority(newPriority);
        return FileManager.updateLine(FileManager.ISSUES, issueId, issue.toFileString());
    }

    // ── Manager assigns an issue: separate customerNote + schedulerNote ──
    /**
     * @param customerNote  Message shown to the customer about the issue status.
     * @param schedulerNote Private instructions for the scheduler.
     */
    public String assignIssue(String issueId, String assignedToStaffId,
                               String customerNote, String schedulerNote) {
        if (customerNote == null || customerNote.trim().isEmpty())
            return "A response to the customer is required.";
        if (schedulerNote == null || schedulerNote.trim().isEmpty())
            return "Instructions for the scheduler are required.";

        IssueAssignment existing = getAssignmentByIssue(issueId);
        String managerId = SessionManager.getInstance().getCurrentStaff().getStaffId();

        String assignId;
        if (existing != null) {
            existing.setAssignedToStaffId(assignedToStaffId);
            existing.setAssignedByManagerId(managerId);
            existing.setAssignedAt(DateTimeUtils.now());
            existing.setCustomerNote(customerNote);
            existing.setSchedulerNote(schedulerNote);
            existing.setCompletedAt("NULL");
            assignId = existing.getAssignId();
            FileManager.updateLine(FileManager.ISSUE_ASSIGN, assignId, existing.toFileString());
        } else {
            assignId = FileManager.getNextId("IA", FileManager.ISSUE_ASSIGN);
            IssueAssignment assignment = new IssueAssignment(
                    assignId, issueId, assignedToStaffId, managerId,
                    DateTimeUtils.now(), customerNote, schedulerNote, "NULL");
            FileManager.appendLine(FileManager.ISSUE_ASSIGN, assignment.toFileString());
        }

        updateIssueStatus(issueId, "IN_PROGRESS");
        ActivityLogService.logIssueAssigned(managerId, issueId, assignedToStaffId);

        // Notify the scheduler
        notifService.notifySchedulerAssigned(assignedToStaffId, managerId, issueId, assignId);
        return null;
    }

    /** Backward-compat: single response goes to customerNote, schedulerNote = same text. */
    public String assignIssue(String issueId, String assignedToStaffId, String response) {
        return assignIssue(issueId, assignedToStaffId, response, response);
    }

    // ── Scheduler marks issue done + sends resolution note to manager ──
    /**
     * @param assignId       The assignment being completed.
     * @param resolutionNote The scheduler's note sent back to the manager.
     */
    public boolean completeAssignment(String assignId, String resolutionNote) {
        String line = FileManager.findById(FileManager.ISSUE_ASSIGN, assignId);
        if (line == null) return false;
        IssueAssignment a = IssueAssignment.fromString(line);

        // schedulerNote field now holds the scheduler's resolution note to manager
        a.setSchedulerNote(resolutionNote);
        a.setCompletedAt(DateTimeUtils.now());
        boolean ok = FileManager.updateLine(FileManager.ISSUE_ASSIGN, assignId, a.toFileString());

        // Notify manager
        if (ok) {
            String schedulerId = a.getAssignedToStaffId();
            String managerId   = a.getAssignedByManagerId();
            notifService.notifyManagerResolved(managerId, schedulerId,
                    a.getIssueId(), assignId, resolutionNote);
        }
        return ok;
    }

    // ── Staff performance stats ────────────────────────────────────
    public static class StaffPerformance {
        public final String staffId;
        public final long   assigned, completed;
        public final double avgResolutionHours;
        public StaffPerformance(String id, long a, long c, double avg) {
            staffId = id; assigned = a; completed = c; avgResolutionHours = avg;
        }
    }

    public StaffPerformance getPerformanceByStaff(String staffId) {
        List<IssueAssignment> mine = getAssignmentsByStaff(staffId);
        long assigned  = mine.size();
        long completed = mine.stream().filter(a -> !"NULL".equals(a.getCompletedAt())).count();
        double avgHours = mine.stream()
                .filter(a -> !"NULL".equals(a.getCompletedAt()))
                .mapToDouble(a -> {
                    try {
                        return java.time.temporal.ChronoUnit.HOURS.between(
                                DateTimeUtils.parse(a.getAssignedAt()),
                                DateTimeUtils.parse(a.getCompletedAt()));
                    } catch (Exception e) { return 0; }
                })
                .average().orElse(0);
        return new StaffPerformance(staffId, assigned, completed, avgHours);
    }

    // ── Assignment lookups ─────────────────────────────────────────
    public IssueAssignment getAssignmentByIssue(String issueId) {
        List<String> lines = FileManager.findByField(FileManager.ISSUE_ASSIGN, 1, issueId);
        return lines.isEmpty() ? null : IssueAssignment.fromString(lines.get(0));
    }

    public List<IssueAssignment> getAllAssignments() {
        List<IssueAssignment> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.ISSUE_ASSIGN))
            list.add(IssueAssignment.fromString(line));
        return list;
    }

    public List<IssueAssignment> getAssignmentsByStaff(String staffId) {
        List<IssueAssignment> list = new ArrayList<>();
        for (String line : FileManager.findByField(FileManager.ISSUE_ASSIGN, 2, staffId))
            list.add(IssueAssignment.fromString(line));
        return list;
    }
}
