package com.bankatm.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, human-readable IDs for accounts, customers, and transactions.
 * Uses AtomicLong so IDs stay unique even under concurrent access.
 */
public final class IdGenerator {
    private static final AtomicLong ACCOUNT_SEQ = new AtomicLong(100000);
    private static final AtomicLong CUSTOMER_SEQ = new AtomicLong(1000);
    private static final AtomicLong TRANSACTION_SEQ = new AtomicLong(1);

    private IdGenerator() {
        // utility class, no instances
    }

    public static String nextAccountNumber() {
        return "ACC" + ACCOUNT_SEQ.incrementAndGet();
    }

    public static String nextCustomerId() {
        return "CUST" + CUSTOMER_SEQ.incrementAndGet();
    }

    public static String nextTransactionId() {
        return "TXN" + String.format("%08d", TRANSACTION_SEQ.getAndIncrement());
    }
}
