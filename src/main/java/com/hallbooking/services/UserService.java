package com.hallbooking.services;

import com.hallbooking.models.Customer;
import com.hallbooking.models.Staff;
import com.hallbooking.models.User;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.PasswordUtils;
import com.hallbooking.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * UserService — used by Administrator to manage staff and customer accounts.
 */
public class UserService {

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.USERS))
            list.add(User.fromString(line));
        return list;
    }

    public List<User> getAllCustomerUsers() {
        List<User> list = new ArrayList<>();
        for (User u : getAllUsers())
            if ("CUSTOMER".equals(u.getRole())) list.add(u);
        return list;
    }

    public List<User> getAllStaffUsers() {
        List<User> list = new ArrayList<>();
        for (User u : getAllUsers())
            if (!"CUSTOMER".equals(u.getRole())) list.add(u);
        return list;
    }

    public List<Staff> getAllStaff() {
        List<Staff> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.STAFF))
            list.add(Staff.fromString(line));
        return list;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        for (String line : FileManager.readLines(FileManager.CUSTOMERS))
            list.add(Customer.fromString(line));
        return list;
    }

    public List<Staff> getSchedulers() {
        List<Staff> list = new ArrayList<>();
        for (Staff s : getAllStaff())
            if ("SCHEDULING".equalsIgnoreCase(s.getDepartment())) list.add(s);
        return list;
    }

    // ── Create staff account (Admin only) ─────────────────────────
    public String createStaffAccount(String fullName, String email, String password,
                                     String phone, String role, String department) {
        if (!role.equals("SCHEDULER") && !role.equals("MANAGER"))
            return "Role must be SCHEDULER or MANAGER.";
        if (fullName.trim().isEmpty()) return "Full name is required.";
        if (email.trim().isEmpty())    return "Email is required.";
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            return "Invalid email address.";
        if (password.length() < 6)    return "Password must be at least 6 characters.";

        for (User u : getAllUsers())
            if (u.getEmail().equalsIgnoreCase(email)) return "Email already in use.";

        if (department == null || department.trim().isEmpty()) {
            department = role.equals("SCHEDULER") ? "Scheduling" : "Management";
        }

        String adminId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        String userId  = FileManager.getNextId("U", FileManager.USERS);
        String staffId = FileManager.getNextId("S", FileManager.STAFF);

        User user = new User(userId, email, PasswordUtils.hash(password), phone,
                role, true, DateTimeUtils.now(), DateTimeUtils.now());
        FileManager.appendLine(FileManager.USERS, user.toFileString());

        Staff staff = new Staff(staffId, userId, fullName, department,
                DateTimeUtils.today(), true, adminId);
        FileManager.appendLine(FileManager.STAFF, staff.toFileString());
        ActivityLogService.logStaffCreated(adminId, staffId, fullName);
        return null;
    }

    // ── Reset password (Admin only) ───────────────────────────────
    public String resetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6)
            return "Password must be at least 6 characters.";
        String line = FileManager.findById(FileManager.USERS, userId);
        if (line == null) return "User not found.";
        User user = User.fromString(line);
        user.setPassword(PasswordUtils.hash(newPassword));
        FileManager.updateLine(FileManager.USERS, userId, user.toFileString());
        String adminId = SessionManager.getInstance().getCurrentStaff() != null
                ? SessionManager.getInstance().getCurrentStaff().getStaffId() : "SYSTEM";
        ActivityLogService.logPasswordReset(adminId, userId);
        return null; // success
    }

    // ── Block / Unblock / Delete ───────────────────────────────────
    public boolean blockUser(String userId) {
        String line = FileManager.findById(FileManager.USERS, userId);
        if (line == null) return false;
        User user = User.fromString(line);
        user.setActive(false);
        boolean result = FileManager.updateLine(FileManager.USERS, userId, user.toFileString());
        if (result) {
            String adminId = SessionManager.getInstance().getCurrentStaff() != null
                    ? SessionManager.getInstance().getCurrentStaff().getStaffId() : "SYSTEM";
            ActivityLogService.logUserBlocked(adminId, userId);
        }
        return result;
    }

    public boolean unblockUser(String userId) {
        String line = FileManager.findById(FileManager.USERS, userId);
        if (line == null) return false;
        User user = User.fromString(line);
        user.setActive(true);
        boolean result = FileManager.updateLine(FileManager.USERS, userId, user.toFileString());
        if (result) {
            String adminId = SessionManager.getInstance().getCurrentStaff() != null
                    ? SessionManager.getInstance().getCurrentStaff().getStaffId() : "SYSTEM";
            ActivityLogService.logUserUnblocked(adminId, userId);
        }
        return result;
    }

    public boolean deleteUser(String userId) {
        String userLine = FileManager.findById(FileManager.USERS, userId);
        if (userLine == null) return false;
        User user = User.fromString(userLine);

        if ("CUSTOMER".equals(user.getRole())) {
            List<String> custLines = FileManager.readLines(FileManager.CUSTOMERS);
            custLines.removeIf(l -> l.split("\\|")[1].equals(userId));
            FileManager.writeLines(FileManager.CUSTOMERS, custLines);
        } else {
            List<String> staffLines = FileManager.readLines(FileManager.STAFF);
            staffLines.removeIf(l -> l.split("\\|")[1].equals(userId));
            FileManager.writeLines(FileManager.STAFF, staffLines);
        }
        return FileManager.deleteLine(FileManager.USERS, userId);
    }

    // ── Update records ─────────────────────────────────────────────
    public boolean updateStaff(Staff staff) {
        return FileManager.updateLine(FileManager.STAFF, staff.getStaffId(), staff.toFileString());
    }

    /** Update a User record — used by Admin when editing staff details. */
    public boolean updateUser(User user) {
        return FileManager.updateLine(FileManager.USERS, user.getUserId(), user.toFileString());
    }

    // ── Lookups ────────────────────────────────────────────────────
    public User getUserById(String userId) {
        String line = FileManager.findById(FileManager.USERS, userId);
        return line != null ? User.fromString(line) : null;
    }

    public Staff getStaffById(String staffId) {
        String line = FileManager.findById(FileManager.STAFF, staffId);
        return line != null ? Staff.fromString(line) : null;
    }

    public Staff getStaffByUserId(String userId) {
        List<String> lines = FileManager.findByField(FileManager.STAFF, 1, userId);
        return lines.isEmpty() ? null : Staff.fromString(lines.get(0));
    }

    public List<User> searchUsers(String keyword) {
        String kw = keyword.toLowerCase();
        List<User> result = new ArrayList<>();
        for (User u : getAllUsers())
            if (u.getEmail().toLowerCase().contains(kw)) result.add(u);
        return result;
    }
}
