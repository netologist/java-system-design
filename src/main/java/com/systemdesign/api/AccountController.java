package com.systemdesign.api;

import com.systemdesign.apidesign.ApiResponse;
import com.systemdesign.persistence.Account;
import com.systemdesign.persistence.AccountRepository;
import com.systemdesign.persistence.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * REST API exposing system design architecture patterns.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    public AccountController(AccountService accountService, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    public record CreateAccountRequest(
            @NotBlank String accountNumber,
            @NotBlank String ownerName,
            @Positive long initialBalanceCents
    ) {}

    public record AccountDto(
            String accountNumber,
            String ownerName,
            long balanceCents,
            Long version,
            Instant createdAt
    ) {
        public static AccountDto from(Account a) {
            return new AccountDto(a.getAccountNumber(), a.getOwnerName(), a.getBalanceCents(), a.getVersion(), a.getCreatedAt());
        }
    }

    public record UpdateBalanceRequest(
            @Positive long amountCents,
            boolean isCredit
    ) {}

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest req) {
        Account created = accountService.createAccount(req.accountNumber(), req.ownerName(), req.initialBalanceCents());
        String correlationId = MDC.get("correlationId");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(AccountDto.from(created), correlationId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.findAccount(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        String correlationId = MDC.get("correlationId");
        return ResponseEntity.ok(ApiResponse.ok(AccountDto.from(account), correlationId));
    }

    @PostMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<AccountDto>> updateBalance(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateBalanceRequest req
    ) {
        Account updated = accountService.updateBalanceWithOptimisticRetry(accountNumber, req.amountCents(), req.isCredit(), 3);
        String correlationId = MDC.get("correlationId");
        return ResponseEntity.ok(ApiResponse.ok(AccountDto.from(updated), correlationId));
    }

    @GetMapping("/keyset")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getKeysetPage(
            @RequestParam(required = false) Instant afterCreatedAt,
            @RequestParam(required = false, defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Instant after = (afterCreatedAt == null) ? Instant.EPOCH : afterCreatedAt;
        List<Account> page = accountRepository.findNextKeysetPage(after, afterId, PageRequest.of(0, limit));
        List<AccountDto> dtos = page.stream().map(AccountDto::from).toList();
        String correlationId = MDC.get("correlationId");
        return ResponseEntity.ok(ApiResponse.ok(dtos, correlationId));
    }
}
