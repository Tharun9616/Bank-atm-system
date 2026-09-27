package com.bankatm.ui;

import com.bankatm.exception.BankingException;
import com.bankatm.model.Account;
import com.bankatm.model.AccountType;
import com.bankatm.model.Customer;
import com.bankatm.model.Transaction;
import com.bankatm.service.BankService;
import com.bankatm.util.IdGenerator;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Simple text-based menu that drives the BankService, simulating an ATM/teller terminal.
 */
public class ConsoleATM {

    private final BankService bankService;
    private final Scanner scanner;

    public ConsoleATM(BankService bankService, Scanner scanner) {
        this.bankService = bankService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("=================================================");
        System.out.println("     WELCOME TO JAVA BANK - ATM SIMULATION");
        System.out.println("=================================================");

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> createAccountFlow();
                    case "2" -> sessionFlow();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option, please try again.");
                }
            } catch (BankingException e) {
                System.out.println("ERROR: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
        System.out.println("Thank you for banking with us. Goodbye!");
    }

    private void printMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Open a new account");
        System.out.println("2. Login to existing account");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private void createAccountFlow() {
        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();

        Customer customer = new Customer(IdGenerator.nextCustomerId(), name, email, phone);

        System.out.print("Account type (1=SAVINGS, 2=CHECKING): ");
        AccountType type = scanner.nextLine().trim().equals("1") ? AccountType.SAVINGS : AccountType.CHECKING;

        System.out.print("Set a 4-digit PIN: ");
        String pin = scanner.nextLine().trim();

        System.out.print("Opening deposit (min 100.00): ");
        BigDecimal deposit = new BigDecimal(scanner.nextLine().trim());

        Account account = bankService.openAccount(customer, type, pin, deposit);
        System.out.println("Account created successfully!");
        System.out.println("Your account number is: " + account.getAccountNumber());
        System.out.println("(Keep this safe - you'll need it, along with your PIN, to log in.)");
    }

    private void sessionFlow() {
        System.out.print("Account number: ");
        String accountNumber = scanner.nextLine().trim();
        System.out.print("PIN: ");
        String pin = scanner.nextLine().trim();

        Account account = bankService.authenticate(accountNumber, pin);
        System.out.println("Login successful. Welcome, " + account.getOwner().getFullName() + "!");

        boolean loggedIn = true;
        while (loggedIn) {
            printAccountMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> System.out.println("Current balance: " + bankService.getBalance(accountNumber));
                    case "2" -> depositFlow(accountNumber);
                    case "3" -> withdrawFlow(accountNumber);
                    case "4" -> transferFlow(accountNumber);
                    case "5" -> printStatement(accountNumber);
                    case "0" -> loggedIn = false;
                    default -> System.out.println("Invalid option, please try again.");
                }
            } catch (BankingException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void printAccountMenu() {
        System.out.println("\n--- ACCOUNT MENU ---");
        System.out.println("1. Check balance");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer to another account");
        System.out.println("5. Mini statement");
        System.out.println("0. Logout");
        System.out.print("Choose an option: ");
    }

    private void depositFlow(String accountNumber) {
        System.out.print("Amount to deposit: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        Transaction txn = bankService.deposit(accountNumber, amount);
        System.out.println("Deposit successful. " + txn);
    }

    private void withdrawFlow(String accountNumber) {
        System.out.print("Amount to withdraw: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        Transaction txn = bankService.withdraw(accountNumber, amount);
        System.out.println("Withdrawal successful. " + txn);
    }

    private void transferFlow(String accountNumber) {
        System.out.print("Recipient account number: ");
        String toAccount = scanner.nextLine().trim();
        System.out.print("Amount to transfer: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        bankService.transfer(accountNumber, toAccount, amount);
        System.out.println("Transfer of " + amount + " to " + toAccount + " completed successfully.");
    }

    private void printStatement(String accountNumber) {
        System.out.println("\n--- MINI STATEMENT ---");
        var history = bankService.getStatement(accountNumber);
        if (history.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            history.forEach(System.out::println);
        }
    }
}
