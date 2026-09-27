# Bank / ATM Simulation System (Java)

A console-based banking system that models real ATM/teller operations: account
creation, authentication, deposits, withdrawals, and inter-account transfers —
built with clean OOP layering, custom exceptions, and thread-safe transaction
handling with rollback on failure.

## Features

- **Account management** — open savings/checking accounts, PIN-based login (PINs are SHA-256 hashed, never stored in plain text)
- **Core transactions** — deposit, withdraw, transfer, balance inquiry, mini statement
- **Data integrity** — every operation validates inputs and throws a specific custom exception on failure (`InsufficientFundsException`, `InvalidAmountException`, `InvalidPinException`, `AccountNotFoundException`, `DuplicateAccountException`)
- **Transaction rollback** — if a transfer's credit step fails after the debit already succeeded, the debit is automatically refunded so funds are never lost in transit
- **Thread safety** — each account has its own lock; transfers lock both accounts in a fixed, consistent order to prevent deadlocks between concurrent transfers running in opposite directions
- **Layered architecture** — model / repository / service / ui, so storage (currently in-memory) can be swapped for JDBC/JPA without touching business logic
- **Dependency-free test suite** — 9 tests covering happy paths, error paths, and concurrency (including a deliberate bidirectional-transfer deadlock stress test)

## Architecture

```
com.bankatm
├── Main.java                  Entry point
├── model/                     Account, Customer, Transaction, enums
├── exception/                 Custom checked-style business exceptions
├── repository/                Storage abstraction (AccountRepository) + in-memory impl
├── service/                   BankService — all business logic & transaction safety
└── ui/                        ConsoleATM — text menu that drives BankService
```

**Design decisions worth mentioning in an interview:**
1. **Repository pattern** — `BankService` depends on the `AccountRepository` interface, not a concrete class, so swapping in a MySQL/JDBC-backed repository later requires zero changes to business logic.
2. **Immutable `Transaction` records** — once created, a transaction can't be edited, which is how a real audit trail should behave.
3. **Locking strategy for transfers** — locks are acquired in a fixed order (by account number) rather than "from account first", which is what actually prevents deadlock when two transfers run in opposite directions concurrently. This is demonstrated in `testConcurrentTransfersNoDeadlockNoFundsLost`.
4. **Manual rollback** — since there's no real database transaction here, `BankService.transfer()` manually reverses the debit if the credit step throws, mirroring what a `@Transactional` annotation / DB rollback would do in a Spring + JPA version.

## How to run

**Option A — plain `javac` (no Maven/internet required):**
```bash
javac -d target/classes $(find src/main -name "*.java")
java -cp target/classes com.bankatm.Main
```

**Option B — Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="com.bankatm.Main"
```

## How to run the tests

```bash
javac -cp target/classes -d target/test-classes src/test/java/com/bankatm/BankServiceTest.java
java -cp target/classes:target/test-classes com.bankatm.BankServiceTest
```
Expected output: `RESULTS: 9 passed, 0 failed`

## Possible extensions (good talking points / future work)

- Swap `InMemoryAccountRepository` for a JDBC repository backed by MySQL/PostgreSQL
- Add a Spring Boot REST API layer on top of the same `BankService`
- Add interest accrual scheduling for savings accounts (`AccountType` already carries an interest rate)
- Add JWT-based auth and role-based access control (admin vs. customer) if exposed as a web API
- Persist transaction history to a file/DB so it survives restarts

## Suggested resume bullet points

- "Built a Java banking simulation with layered architecture (model/repository/service/UI), custom exception hierarchy, and thread-safe transaction handling using explicit locking with deadlock-avoidance ordering."
- "Implemented atomic fund transfers with manual rollback-on-failure logic, verified via concurrency tests simulating 20 simultaneous bidirectional transfers with zero fund loss."
- "Designed a repository abstraction layer enabling the storage backend to be swapped (in-memory → JDBC) without changes to business logic."
