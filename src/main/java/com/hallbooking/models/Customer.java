package com.hallbooking.models;

/**
 * Customer — a registered customer who can browse and book halls.
 * Extends Person, inheriting fullName and phone.
 */
public class Customer extends Person {

    private String customerId;
    private String userId;
    // fullName and phone are inherited from Person
    private String address;

    public Customer() {}

    public Customer(String customerId, String userId,
                    String fullName, String address, String phone) {
        super(fullName, phone);
        this.customerId = customerId;
        this.userId     = userId;
        this.address    = address;
    }

    public static Customer fromString(String line) {
        String[] p = line.split("\\|");
        return new Customer(p[0], p[1], p[2], p[3], p[4]);
    }

    @Override
    public String toFileString() {
        return String.join("|", customerId, userId, fullName, address, phone);
    }

    // Getters
    public String getCustomerId() { return customerId; }
    public String getUserId()     { return userId; }
    // getFullName() and getPhone() inherited from Person
    public String getAddress()    { return address; }

    // Setters
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setUserId(String userId)         { this.userId     = userId; }
    // setFullName() and setPhone() inherited from Person
    public void setAddress(String address)       { this.address    = address; }

    @Override
    public String toString() {
        return "Customer{" + customerId + ", " + fullName + "}";
    }
}
