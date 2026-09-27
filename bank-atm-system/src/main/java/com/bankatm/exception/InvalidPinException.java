package com.bankatm.exception;

/** Thrown when authentication fails due to an incorrect PIN. */
public class InvalidPinException extends BankingException {
    public InvalidPinException(String message) {
        super(message);
    }
}
