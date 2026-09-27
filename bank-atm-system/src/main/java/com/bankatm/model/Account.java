package com.bankatm.model;

import com.bankatm.exception.InsufficientFundsException;
import com.bankatm.exception.InvalidAmountException;
import com.bankatm.util.IdGenerator;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a bank account. All balance-mutating operations are guarded by a
 * {@link ReentrantLock} so concurrent ATM/teller operations on the same account
 * cannot corrupt the balance (classic race-condition protection).
 *
 * The PIN is never stored in plain text - only its SHA-256 hash is kept.
 */
public class Account {
    private final String accountNumber;
    private final Customer owner;
    private final AccountType accountType;
    private final String pinHash;
    private BigDecimal balance;
    private boolean closed = false;

    private final List<Transaction> transactionHistory = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Account(String accountNumber, Customer owner, AccountType accountType,
                    String pin, BigDecimal openingBalance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.accountType = accountType;
        this.pinHash = hashPin(pin);
        this.balance = openingBalance;
    }

    /** Acquire this account's lock. Used by BankService for multi-account operations like transfers. */
    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean verifyPin(String pin) {
        return pinHash.equals(hashPin(pin));
    }

    private static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Credits the account. Caller (BankService) is expected to hold the lock
     * when this is part of a multi-step transaction (e.g. a transfer).
     */
    public Transaction credit(BigDecimal amount, TransactionType type, String description) {
        validateAmount(amount);
        balance = balance.add(amount);
        Transaction txn = new Transaction(IdGenerator.nextTransactionId(), type, amount, balance, description);
        transactionHistory.add(txn);
        return txn;
    }

    /**
     * Debits the account after checking sufficient funds.
     * Throws InsufficientFundsException without mutating state if funds are insufficient
     * (ensures the account is never left in an inconsistent state).
     */
    public Transaction debit(BigDecimal amount, TransactionType type, String description) {
        validateAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds in account %s: balance=%s, requested=%s",
                            accountNumber, balance, amount));
        }
        balance = balance.subtract(amount);
        Transaction txn = new Transaction(IdGenerator.nextTransactionId(), type, amount, balance, description);
        transactionHistory.add(txn);
        return txn;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Transaction amount must be positive, got: " + amount);
        }
        if (closed) {
            throw new IllegalStateException("Account " + accountNumber + " is closed");
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getOwner() {
        return owner;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    @Override
    public String toString() {
        return String.format("Account{number='%s', owner='%s', type=%s, balance=%s}",
                accountNumber, owner.getFullName(), accountType, balance);
    }
}
