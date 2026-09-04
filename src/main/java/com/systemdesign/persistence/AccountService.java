package com.systemdesign.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service demonstrating Optimistic Locking Retry Pattern and Transaction Boundaries.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(String accountNumber, String owner, long initialCents) {
        Account account = new Account(accountNumber, owner, initialCents);
        return accountRepository.save(account);
    }

    /**
     * Performs a balance update with Optimistic Concurrency Control.
     * Retries automatically if a concurrent modification occurs.
     */
    public Account updateBalanceWithOptimisticRetry(String accountNumber, long amountCents, boolean isCredit, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeSingleBalanceUpdate(accountNumber, amountCents, isCredit);
            } catch (OptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict on account {} (attempt {}/{})", accountNumber, attempt, maxAttempts);
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("Failed to update balance after " + maxAttempts + " attempts due to concurrent updates", ex);
                }
                try {
                    Thread.sleep(20L * attempt); // Backoff with jitter
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during optimistic lock retry", ie);
                }
            }
        }
        throw new IllegalStateException("Exhausted retry attempts");
    }

    @Transactional
    protected Account executeSingleBalanceUpdate(String accountNumber, long amountCents, boolean isCredit) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        if (isCredit) {
            account.credit(amountCents);
        } else {
            account.debit(amountCents);
        }

        return accountRepository.save(account);
    }

    public Optional<Account> findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
}
