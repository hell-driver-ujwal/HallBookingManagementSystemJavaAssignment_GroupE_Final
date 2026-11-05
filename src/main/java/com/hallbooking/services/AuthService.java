package com.hallbooking.services;

import com.hallbooking.models.Customer;
import com.hallbooking.models.Staff;
import com.hallbooking.models.User;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.PasswordUtils;
import com.hallbooking.utils.SessionManager;

import java.util.List;

/**
 * AuthService — handles login, logout, and registration.
 *
 * Two separate login portals:
 *   loginCustomer(email, password)  → only CUSTOMER role allowed
 *   loginStaff(email, password)     → SCHEDULER / ADMIN / MANAGER only
 *
 * Registration:
 *   registerCustomer(...)           → creates USER + CUSTOMER records
 */
public class AuthService {

    // ── Customer login ─────────────────────────────────────────────────
    // Returns null on success (session is set), or an error message string.
    public String loginCustomer(String email, String password) {
        User user = findUserByEmail(email);
        if (user == null)
            return "No account found with that email.";
        if (!PasswordUtils.verify(password, user.getPassword()))
            return "Incorrect password.";
        if (!user.isActive())
            return "Your account has been blocked. Please contact support.";
        if (!"CUSTOMER".equals(user.getRole()))
            return "This login is for customers only.\nPlease use the Staff login.";

        // Load matching Customer profile
        Customer customer = findCustomerByUserId(user.getUserId());
        if (customer == null)
            return "Customer profile not found. Please contact support.";

        // Set session
        SessionManager.getInstance().setCurrentUser(user);
        SessionManager.getInstance().setCurrentCustomer(customer);

        // Update lastLoginAt
        updateLastLogin(user);
        ActivityLogService.logLogin(user.getUserId(), user.getRole(), user.getEmail());
        return null; // null = success
    }

    // ── Staff login (Scheduler / Admin / Manager) ──────────────────────
    public String loginStaff(String email, String password) {
        User user = findUserByEmail(email);
        if (user == null)
            return "No staff account found with that email.";
        if (!PasswordUtils.verify(password, user.getPassword()))
            return "Incorrect password.";
        if (!user.isActive())
            return "Your account has been deactivated. Please contact your administrator.";
        if ("CUSTOMER".equals(user.getRole()))
            return "This login is for staff only.\nPlease use the Customer login.";

        // Load matching Staff profile
        Staff staff = findStaffByUserId(user.getUserId());
        if (staff == null)
            return "Staff profile not found. Please contact your administrator.";

        // Set session
        SessionManager.getInstance().setCurrentUser(user);
        SessionManager.getInstance().setCurrentStaff(staff);

        // Update lastLoginAt
        updateLastLogin(user);
        ActivityLogService.logLogin(user.getUserId(), user.getRole(), user.getEmail());
        return null;
    }

    // ── Customer registration ──────────────────────────────────────────
    public String registerCustomer(String fullName, String email, String password,
                                   String confirmPassword, String phone, String address) {
        // Validate
        if (fullName.trim().isEmpty()) return "Full name is required.";
        if (email.trim().isEmpty())    return "Email is required.";
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            return "Please enter a valid email address.";
        if (password.length() < 6)    return "Password must be at least 6 characters.";
        if (!password.equals(confirmPassword)) return "Passwords do not match.";
        if (!phone.matches("^0[0-9]{8,10}$"))
            return "Phone must be 9–11 digits starting with 0.";

        // Check duplicate email
        if (findUserByEmail(email) != null)
            return "An account with this email already exists.";

        // Generate IDs
        String userId     = FileManager.getNextId("U", FileManager.USERS);
        String customerId = FileManager.getNextId("C", FileManager.CUSTOMERS);

        // Create User record
        User user = new User(userId, email, PasswordUtils.hash(password), phone,
                "CUSTOMER", true, DateTimeUtils.now(), DateTimeUtils.now());
        FileManager.appendLine(FileManager.USERS, user.toFileString());

        // Create Customer record
        Customer customer = new Customer(customerId, userId, fullName, address, phone);
        FileManager.appendLine(FileManager.CUSTOMERS, customer.toFileString());

        return null; // null = success
    }

    // ── Logout ─────────────────────────────────────────────────────────
    public void logout() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null)
            ActivityLogService.logLogout(user.getUserId(), user.getRole());
        SessionManager.getInstance().logout();
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private User findUserByEmail(String email) {
        for (String line : FileManager.readLines(FileManager.USERS)) {
            User u = User.fromString(line);
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    private Customer findCustomerByUserId(String userId) {
        List<String> lines = FileManager.findByField(FileManager.CUSTOMERS, 1, userId);
        return lines.isEmpty() ? null : Customer.fromString(lines.get(0));
    }

    private Staff findStaffByUserId(String userId) {
        List<String> lines = FileManager.findByField(FileManager.STAFF, 1, userId);
        return lines.isEmpty() ? null : Staff.fromString(lines.get(0));
    }

    private void updateLastLogin(User user) {
        user.setLastLoginAt(DateTimeUtils.now());
        FileManager.updateLine(FileManager.USERS, user.getUserId(), user.toFileString());
    }
}
