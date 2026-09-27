package com.bankatm.exception;

/** Thrown when a transaction amount is null, zero, negative, or otherwise invalid. */
public class InvalidAmountException extends BankingException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
