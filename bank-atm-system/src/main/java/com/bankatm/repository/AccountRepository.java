package com.bankatm.repository;

import com.bankatm.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over account storage. Programming to this interface (rather than
 * a concrete storage class) means the storage backend can later be swapped for
 * a JDBC/JPA-backed implementation without touching the service layer.
 */
public interface AccountRepository {
    void save(Account account);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAll();

    List<Account> findByCustomerId(String customerId);
}
