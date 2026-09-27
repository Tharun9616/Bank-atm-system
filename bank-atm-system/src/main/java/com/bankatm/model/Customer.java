package com.bankatm.model;

import java.util.Objects;

/**
 * Represents a bank customer.
 */
public class Customer {
    private final String customerId;
    private final String fullName;
    private final String email;
    private final String phoneNumber;

    public Customer(String customerId, String fullName, String email, String phoneNumber) {
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return String.format("Customer{id='%s', name='%s', email='%s'}", customerId, fullName, email);
    }
}
