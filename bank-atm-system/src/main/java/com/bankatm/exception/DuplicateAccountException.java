package com.bankatm.exception;

/** Thrown when attempting to create an account with an ID that already exists. */
public class DuplicateAccountException extends BankingException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
