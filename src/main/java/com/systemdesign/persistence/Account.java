package com.systemdesign.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Account Entity demonstrating:
 * <ul>
 *   <li><b>Optimistic Locking:</b> Via {@link Version} to prevent lost updates under concurrency.</li>
 *   <li><b>Soft Delete:</b> Via {@link SQLDelete} and {@link SQLRestriction} to preserve audit trails.</li>
 *   <li><b>Financial Precision:</b> Balance stored in minor units (cents/kuruş) as {@code long}.</li>
 * </ul>
 */
@Entity
@Table(name = "accounts")
@SQLDelete(sql = "UPDATE accounts SET deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private long balanceCents;

    @Version
    private Long version;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {}

    public Account(String accountNumber, String ownerName, long initialBalanceCents) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balanceCents = initialBalanceCents;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public long getBalanceCents() { return balanceCents; }
    public Long getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }

    public void credit(long amountCents) {
        if (amountCents <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        this.balanceCents += amountCents;
    }

    public void debit(long amountCents) {
        if (amountCents <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (this.balanceCents < amountCents) {
            throw new IllegalStateException("Insufficient funds in account: " + accountNumber);
        }
        this.balanceCents -= amountCents;
    }
}
