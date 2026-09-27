package com.bankatm.repository;

import com.bankatm.model.Account;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation backed by a ConcurrentHashMap, so it is safe to
 * use from multiple threads (e.g. simulating multiple ATMs hitting the same
 * bank server concurrently).
 */
public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void save(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(accounts.values());
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return accounts.values().stream()
                .filter(a -> a.getOwner().getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }
}
