package com.bankatm.model;

/**
 * Types of bank accounts supported by the system.
 */
public enum AccountType {
    SAVINGS(0.03),   // 3% annual interest (illustrative)
    CHECKING(0.0);   // no interest

    private final double annualInterestRate;

    AccountType(double annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }
}
