package com.bankatm.exception;

/**
 * Base unchecked exception for all banking domain errors.
 * Kept unchecked so the console UI layer can catch a single common type
 * while service code can still throw specific subtypes for precise handling.
 */
public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
