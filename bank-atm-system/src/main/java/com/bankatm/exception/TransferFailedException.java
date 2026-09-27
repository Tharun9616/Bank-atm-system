package com.bankatm.exception;

/**
 * Thrown when a transfer could not be completed and had to be rolled back.
 * The cause chain contains the original failure reason.
 */
public class TransferFailedException extends BankingException {
    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
