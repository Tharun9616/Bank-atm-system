package com.bankatm.service;

import com.bankatm.exception.*;
import com.bankatm.model.*;
import com.bankatm.repository.AccountRepository;
import com.bankatm.util.IdGenerator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core banking business logic: account creation, authentication, deposits,
 * withdrawals, transfers and statements.
 *
 * Concurrency & consistency notes:
 *  - Every account carries its own lock (see Account.lock()/unlock()).
 *  - Single-account operations (deposit/withdraw) lock just that account.
 *  - Transfers lock BOTH accounts. To prevent deadlock when two threads
 *    transfer between the same two accounts in opposite directions, locks
 *    are always acquired in a fixed order (by account number, lexicographically).
 *  - If the debit succeeds but the credit fails for any reason, the debit is
 *    rolled back (refunded) before the exception propagates, so the customer's
 *    money is never "lost in transit". This mirrors the atomicity guarantee a
 *    real system would get from a database transaction / two-phase commit.
 */
public class BankService {

    private final AccountRepository accountRepository;
    private static final BigDecimal MIN_OPENING_BALANCE = new BigDecimal("100.00");

    public BankService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account openAccount(Customer customer, AccountType type, String pin, BigDecimal openingDeposit) {
        if (openingDeposit == null || openingDeposit.compareTo(MIN_OPENING_BALANCE) < 0) {
            throw new InvalidAmountException(
                    "Opening deposit must be at least " + MIN_OPENING_BALANCE);
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new InvalidPinException("PIN must be exactly 4 digits");
        }

        String accountNumber = IdGenerator.nextAccountNumber();
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            // Practically unreachable with AtomicLong IDs, but guarded for correctness.
            throw new DuplicateAccountException("Account number collision: " + accountNumber);
        }

        Account account = new Account(accountNumber, customer, type, pin, openingDeposit);
        accountRepository.save(account);
        return account;
    }

    public Account authenticate(String accountNumber, String pin) {
        Account account = getAccountOrThrow(accountNumber);
        if (!account.verifyPin(pin)) {
            throw new InvalidPinException("Incorrect PIN for account " + accountNumber);
        }
        if (account.isClosed()) {
            throw new BankingException("Account " + accountNumber + " is closed");
        }
        return account;
    }

    public Transaction deposit(String accountNumber, BigDecimal amount) {
        Account account = getAccountOrThrow(accountNumber);
        account.lock();
        try {
            return account.credit(amount, TransactionType.DEPOSIT, "Cash deposit");
        } finally {
            account.unlock();
        }
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount) {
        Account account = getAccountOrThrow(accountNumber);
        account.lock();
        try {
            return account.debit(amount, TransactionType.WITHDRAWAL, "Cash withdrawal");
        } finally {
            account.unlock();
        }
    }

    /**
     * Transfers funds between two accounts atomically from the caller's point of view:
     * either both the debit and credit succeed, or neither does.
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        Account from = getAccountOrThrow(fromAccountNumber);
        Account to = getAccountOrThrow(toAccountNumber);

        // Always lock in a fixed, consistent order to prevent deadlock between
        // two concurrent transfers that involve the same pair of accounts.
        Account first = fromAccountNumber.compareTo(toAccountNumber) < 0 ? from : to;
        Account second = fromAccountNumber.compareTo(toAccountNumber) < 0 ? to : from;

        first.lock();
        second.lock();
        try {
            from.debit(amount, TransactionType.TRANSFER_OUT, "Transfer to " + toAccountNumber);
            try {
                to.credit(amount, TransactionType.TRANSFER_IN, "Transfer from " + fromAccountNumber);
            } catch (RuntimeException creditFailure) {
                // ROLLBACK: the debit already happened, so refund it before propagating.
                // This is the manual equivalent of a DB transaction rollback.
                from.credit(amount, TransactionType.DEPOSIT, "Rollback: failed transfer to " + toAccountNumber);
                throw new TransferFailedException(
                        "Transfer from " + fromAccountNumber + " to " + toAccountNumber
                                + " failed and was rolled back", creditFailure);
            }
        } finally {
            second.unlock();
            first.unlock();
        }
    }

    public BigDecimal getBalance(String accountNumber) {
        return getAccountOrThrow(accountNumber).getBalance();
    }

    public List<Transaction> getStatement(String accountNumber) {
        return getAccountOrThrow(accountNumber).getTransactionHistory();
    }

    public void closeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        account.lock();
        try {
            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new BankingException(
                        "Cannot close account " + accountNumber + " with a non-zero balance ("
                                + account.getBalance() + "). Withdraw remaining funds first.");
            }
            account.close();
        } finally {
            account.unlock();
        }
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("No account found with number: " + accountNumber));
    }
}
