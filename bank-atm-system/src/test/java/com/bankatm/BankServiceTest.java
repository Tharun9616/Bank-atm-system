package com.bankatm;

import com.bankatm.exception.InsufficientFundsException;
import com.bankatm.exception.InvalidPinException;
import com.bankatm.model.Account;
import com.bankatm.model.AccountType;
import com.bankatm.model.Customer;
import com.bankatm.repository.AccountRepository;
import com.bankatm.repository.InMemoryAccountRepository;
import com.bankatm.service.BankService;
import com.bankatm.util.IdGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A lightweight, dependency-free test suite. Run with: java -cp target/classes com.bankatm.BankServiceTest
 * (No JUnit required, so it runs in any environment with zero setup - useful for a quick demo/CI check.
 * A pom.xml with JUnit 5 is also included for anyone who wants to run these as real @Test methods in an IDE.)
 */
public class BankServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws InterruptedException {
        testDepositAndWithdraw();
        testInsufficientFundsThrows();
        testInvalidPinThrows();
        testSuccessfulTransfer();
        testTransferToNonexistentAccountRollsBackNothingLost();
        testConcurrentWithdrawalsStayConsistent();
        testConcurrentTransfersNoDeadlockNoFundsLost();

        System.out.println("\n=================================");
        System.out.println("RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("=================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static BankService newService() {
        AccountRepository repo = new InMemoryAccountRepository();
        return new BankService(repo);
    }

    private static Customer newCustomer(String name) {
        return new Customer(IdGenerator.nextCustomerId(), name, name.toLowerCase() + "@mail.com", "1234567890");
    }

    private static void testDepositAndWithdraw() {
        BankService bank = newService();
        Account acc = bank.openAccount(newCustomer("Alice"), AccountType.SAVINGS, "1234", new BigDecimal("500.00"));

        bank.deposit(acc.getAccountNumber(), new BigDecimal("150.00"));
        bank.withdraw(acc.getAccountNumber(), new BigDecimal("200.00"));

        check("Deposit+withdraw balance correct",
                bank.getBalance(acc.getAccountNumber()).compareTo(new BigDecimal("450.00")) == 0);
    }

    private static void testInsufficientFundsThrows() {
        BankService bank = newService();
        Account acc = bank.openAccount(newCustomer("Bob"), AccountType.CHECKING, "1111", new BigDecimal("100.00"));
        try {
            bank.withdraw(acc.getAccountNumber(), new BigDecimal("500.00"));
            check("Overdraw should throw InsufficientFundsException", false);
        } catch (InsufficientFundsException e) {
            // balance must be unchanged
            check("Balance unchanged after failed withdrawal",
                    bank.getBalance(acc.getAccountNumber()).compareTo(new BigDecimal("100.00")) == 0);
        }
    }

    private static void testInvalidPinThrows() {
        BankService bank = newService();
        Account acc = bank.openAccount(newCustomer("Carol"), AccountType.SAVINGS, "4321", new BigDecimal("200.00"));
        try {
            bank.authenticate(acc.getAccountNumber(), "0000");
            check("Wrong PIN should throw InvalidPinException", false);
        } catch (InvalidPinException e) {
            check("Wrong PIN correctly rejected", true);
        }
    }

    private static void testSuccessfulTransfer() {
        BankService bank = newService();
        Account a = bank.openAccount(newCustomer("Dan"), AccountType.CHECKING, "1111", new BigDecimal("300.00"));
        Account b = bank.openAccount(newCustomer("Eve"), AccountType.CHECKING, "2222", new BigDecimal("300.00"));

        bank.transfer(a.getAccountNumber(), b.getAccountNumber(), new BigDecimal("100.00"));

        check("Sender debited correctly",
                bank.getBalance(a.getAccountNumber()).compareTo(new BigDecimal("200.00")) == 0);
        check("Recipient credited correctly",
                bank.getBalance(b.getAccountNumber()).compareTo(new BigDecimal("400.00")) == 0);
    }

    private static void testTransferToNonexistentAccountRollsBackNothingLost() {
        BankService bank = newService();
        Account a = bank.openAccount(newCustomer("Frank"), AccountType.CHECKING, "1111", new BigDecimal("300.00"));
        try {
            bank.transfer(a.getAccountNumber(), "ACC-DOES-NOT-EXIST", new BigDecimal("50.00"));
            check("Transfer to bad account should throw", false);
        } catch (Exception e) {
            // Since the recipient lookup fails before any debit happens, sender's balance
            // must be untouched - proving no money "disappears".
            check("No funds lost when transfer target invalid",
                    bank.getBalance(a.getAccountNumber()).compareTo(new BigDecimal("300.00")) == 0);
        }
    }

    private static void testConcurrentWithdrawalsStayConsistent() throws InterruptedException {
        BankService bank = newService();
        Account acc = bank.openAccount(newCustomer("Grace"), AccountType.SAVINGS, "1234", new BigDecimal("1000.00"));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    bank.withdraw(acc.getAccountNumber(), new BigDecimal("50.00"));
                } catch (InsufficientFundsException ignored) {
                    // acceptable under contention
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        // 10 threads x 50.00 = 500.00 total withdrawn -> balance must be exactly 500.00,
        // never more (which would indicate a race condition double-spending funds).
        check("Concurrent withdrawals never over-draw (thread-safety check)",
                bank.getBalance(acc.getAccountNumber()).compareTo(new BigDecimal("500.00")) == 0);
    }

    private static void testConcurrentTransfersNoDeadlockNoFundsLost() throws InterruptedException {
        BankService bank = newService();
        Account a = bank.openAccount(newCustomer("Heidi"), AccountType.CHECKING, "1111", new BigDecimal("1000.00"));
        Account b = bank.openAccount(newCustomer("Ivan"), AccountType.CHECKING, "2222", new BigDecimal("1000.00"));

        BigDecimal totalBefore = bank.getBalance(a.getAccountNumber()).add(bank.getBalance(b.getAccountNumber()));

        // Fire transfers in BOTH directions simultaneously to try to trigger a deadlock
        // if the lock-ordering logic in BankService.transfer() were wrong.
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 10; i++) {
            pool.submit(() -> {
                try { bank.transfer(a.getAccountNumber(), b.getAccountNumber(), new BigDecimal("10.00")); }
                catch (Exception ignored) {} finally { latch.countDown(); }
            });
            pool.submit(() -> {
                try { bank.transfer(b.getAccountNumber(), a.getAccountNumber(), new BigDecimal("10.00")); }
                catch (Exception ignored) {} finally { latch.countDown(); }
            });
        }
        boolean completedInTime = latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        BigDecimal totalAfter = bank.getBalance(a.getAccountNumber()).add(bank.getBalance(b.getAccountNumber()));

        check("No deadlock under bidirectional concurrent transfers", completedInTime);
        check("Total system funds conserved after concurrent transfers",
                totalBefore.compareTo(totalAfter) == 0);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS - " + description);
        } else {
            failed++;
            System.out.println("  FAIL - " + description);
        }
    }
}
