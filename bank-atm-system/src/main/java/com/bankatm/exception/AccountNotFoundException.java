package com.bankatm.exception;

/** Thrown when an operation references an account number that doesn't exist. */
public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
