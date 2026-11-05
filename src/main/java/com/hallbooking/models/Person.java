package com.hallbooking.models;

/**
 * Person — abstract base class shared by User, Customer, and Staff.
 *
 * Captures the common attributes that all people in the system have:
 * a display name (fullName) and a contact phone number.
 *
 * OOP Concepts demonstrated:
 *   - Abstraction  : Person cannot be instantiated directly
 *   - Inheritance  : User, Customer, Staff all extend Person
 *   - Polymorphism : toFileString() and toString() behave differently per subclass
 */
public abstract class Person {

    protected String fullName;
    protected String phone;

    public Person() {}

    public Person(String fullName, String phone) {
        this.fullName = fullName;
        this.phone    = phone;
    }

    // ── Shared getters ─────────────────────────────────────────────
    public String getFullName() { return fullName; }
    public String getPhone()    { return phone; }

    // ── Shared setters ─────────────────────────────────────────────
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone)       { this.phone    = phone; }

    // ── Abstract methods — every subclass must implement ──────────
    /** Serialize this object to a pipe-separated string for .txt storage. */
    public abstract String toFileString();

    @Override
    public abstract String toString();
}
