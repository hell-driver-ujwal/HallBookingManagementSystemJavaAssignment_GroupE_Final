package com.hallbooking.models;

/**
 * User — represents a login account in the system.
 * Extends Person, inheriting phone. fullName is null for User —
 * the display name lives in the linked Customer or Staff record.
 *
 * Roles: CUSTOMER, SCHEDULER, ADMIN, MANAGER
 */
public class User extends Person {

    private String userId;
    private String email;
    private String password;
    // phone is inherited from Person
    private String role;
    private boolean isActive;
    private String createdAt;
    private String lastLoginAt;

    public User() {}

    public User(String userId, String email, String password, String phone,
                String role, boolean isActive, String createdAt, String lastLoginAt) {
        super(null, phone); // User has no fullName — it lives in Customer/Staff
        this.userId      = userId;
        this.email       = email;
        this.password    = password;
        this.role        = role;
        this.isActive    = isActive;
        this.createdAt   = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static User fromString(String line) {
        String[] p = line.split("\\|");
        return new User(p[0], p[1], p[2], p[3], p[4],
                Boolean.parseBoolean(p[5]), p[6], p[7]);
    }

    @Override
    public String toFileString() {
        return String.join("|", userId, email, password, phone, role,
                String.valueOf(isActive), createdAt, lastLoginAt);
    }

    // Getters
    public String  getUserId()      { return userId; }
    public String  getEmail()       { return email; }
    public String  getPassword()    { return password; }
    // getPhone() inherited from Person
    public String  getRole()        { return role; }
    public boolean isActive()       { return isActive; }
    public String  getCreatedAt()   { return createdAt; }
    public String  getLastLoginAt() { return lastLoginAt; }

    // Setters
    public void setUserId(String userId)           { this.userId      = userId; }
    public void setEmail(String email)             { this.email       = email; }
    public void setPassword(String password)       { this.password    = password; }
    // setPhone() inherited from Person
    public void setRole(String role)               { this.role        = role; }
    public void setActive(boolean active)          { this.isActive    = active; }
    public void setCreatedAt(String createdAt)     { this.createdAt   = createdAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    @Override
    public String toString() {
        return "User{" + userId + ", " + email + ", " + role + "}";
    }
}
