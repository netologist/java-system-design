package com.systemdesign.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository pattern demonstrating:
 * <ul>
 *   <li><b>Pessimistic Locking:</b> Exclusive row lock for high-contention write paths.</li>
 *   <li><b>Keyset Pagination:</b> High-performance pagination using indexed seek (ID/timestamp)
 *       avoiding expensive SQL {@code OFFSET}.</li>
 * </ul>
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Pessimistic Write Lock: SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    /**
     * Keyset pagination: retrieves the next page of accounts created after the given timestamp and ID.
     * Scale: O(1) seek regardless of table size (unlike OFFSET O(N)).
     */
    @Query("""
        SELECT a FROM Account a
        WHERE (a.createdAt > :lastCreatedAt)
           OR (a.createdAt = :lastCreatedAt AND a.id > :lastId)
        ORDER BY a.createdAt ASC, a.id ASC
    """)
    List<Account> findNextKeysetPage(
            @Param("lastCreatedAt") Instant lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}
