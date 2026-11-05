package com.hallbooking.utils;

import com.hallbooking.models.Customer;
import com.hallbooking.models.Staff;
import com.hallbooking.models.User;

/**
 * SessionManager — a singleton that holds the currently logged-in user.
 * Every controller reads from here to know who is logged in and what role they have.
 *
 * Usage:
 *   SessionManager.getInstance().setCurrentUser(user);
 *   SessionManager.getInstance().getCurrentUser();
 *   SessionManager.getInstance().getRole();
 *   SessionManager.getInstance().isCustomer();
 *   SessionManager.getInstance().logout();
 */
public class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    private Customer currentCustomer; // set if role == CUSTOMER
    private Staff currentStaff;       // set if role == SCHEDULER / ADMIN / MANAGER

    // ── Pending payment context — set before navigating to payment screen ──
    private String pendingBookingId;
    private double pendingAmount;
    private String pendingPaymentMethod;

    // ── Confirmed booking — set after payment, read by confirmation screen ──
    private String confirmedBookingId;

    // ── Pending rebook context — set before navigating to Browse Halls ──
    private String rebookHallId;
    private int    rebookStartHour;
    private int    rebookStartMin;
    private int    rebookEndHour;
    private int    rebookEndMin;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    // ── Set session ────────────────────────────────────────────────
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
    }

    public void setCurrentStaff(Staff staff) {
        this.currentStaff = staff;
    }

    // ── Read session ───────────────────────────────────────────────
    public User getCurrentUser()         { return currentUser; }
    public Customer getCurrentCustomer() { return currentCustomer; }
    public Staff getCurrentStaff()       { return currentStaff; }

    public String getUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    public String getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public String getDisplayName() {
        if (currentCustomer != null) return currentCustomer.getFullName();
        if (currentStaff    != null) return currentStaff.getFullName();
        return currentUser  != null ? currentUser.getEmail() : "Unknown";
    }

    // ── Role checks ────────────────────────────────────────────────
    public boolean isCustomer()  { return "CUSTOMER".equals(getRole()); }
    public boolean isScheduler() { return "SCHEDULER".equals(getRole()); }
    public boolean isAdmin()     { return "ADMIN".equals(getRole()); }
    public boolean isManager()   { return "MANAGER".equals(getRole()); }
    public boolean isStaff()     { return isScheduler() || isAdmin() || isManager(); }
    public boolean isLoggedIn()  { return currentUser != null; }

    // ── Clear session (logout) ─────────────────────────────────────
    public void logout() {
        currentUser     = null;
        currentCustomer = null;
        currentStaff    = null;
        pendingBookingId     = null;
        pendingAmount        = 0;
        pendingPaymentMethod = null;
        rebookHallId         = null;
    }

    // ── Pending payment context ────────────────────────────────────
    public void setPendingPayment(String bookingId, double amount, String method) {
        this.pendingBookingId     = bookingId;
        this.pendingAmount        = amount;
        this.pendingPaymentMethod = method;
    }

    public void clearPendingPayment() {
        pendingBookingId     = null;
        pendingAmount        = 0;
        pendingPaymentMethod = null;
    }

    public String getPendingBookingId()     { return pendingBookingId; }
    public double getPendingAmount()        { return pendingAmount; }
    public String getPendingPaymentMethod() { return pendingPaymentMethod; }
    public boolean hasPendingPayment()      { return pendingBookingId != null; }

    // ── Confirmed booking context ──────────────────────────────────
    public void setConfirmedBookingId(String bookingId) { this.confirmedBookingId = bookingId; }
    public String getConfirmedBookingId()               { return confirmedBookingId; }
    public void clearConfirmedBooking()                 { this.confirmedBookingId = null; }

    // ── Rebook context ─────────────────────────────────────────────
    public void setPendingRebook(String hallId, int startHour, int startMin,
                                  int endHour, int endMin) {
        this.rebookHallId    = hallId;
        this.rebookStartHour = startHour;
        this.rebookStartMin  = startMin;
        this.rebookEndHour   = endHour;
        this.rebookEndMin    = endMin;
    }

    public void clearPendingRebook() {
        rebookHallId = null;
    }

    public boolean hasPendingRebook()  { return rebookHallId != null; }
    public String  getRebookHallId()   { return rebookHallId; }
    public int     getRebookStartHour(){ return rebookStartHour; }
    public int     getRebookStartMin() { return rebookStartMin; }
    public int     getRebookEndHour()  { return rebookEndHour; }
    public int     getRebookEndMin()   { return rebookEndMin; }
}
