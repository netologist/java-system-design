package com.systemdesign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.api.AccountController;
import com.systemdesign.apidesign.IdempotencyKeyFilter;
import com.systemdesign.observability.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create account returns ApiResponse envelope and X-Correlation-ID")
    void testCreateAccount() throws Exception {
        var req = new AccountController.CreateAccountRequest("ACC-1001", "Alice Johnson", 100_000);

        mockMvc.perform(post("/api/v1/accounts")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-uuid-123"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accountNumber", is("ACC-1001")))
                .andExpect(jsonPath("$.data.balanceCents", is(100000)))
                .andExpect(jsonPath("$.correlationId", is("test-correlation-uuid-123")));
    }

    @Test
    @DisplayName("IdempotencyKeyFilter intercepts duplicate POST requests")
    void testIdempotencyKeyFilter() throws Exception {
        var req = new AccountController.CreateAccountRequest("ACC-2002", "Bob Smith", 50_000);
        String idempotencyKey = "idemp-key-xyz-789";

        // First attempt: executes normally
        mockMvc.perform(post("/api/v1/accounts")
                        .header(IdempotencyKeyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Lookup", "MISS-IDEMPOTENCY"));

        // Second attempt with exact same key: intercepted by IdempotencyKeyFilter, returns 200 OK
        mockMvc.perform(post("/api/v1/accounts")
                        .header(IdempotencyKeyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Lookup", "HIT-IDEMPOTENCY"))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Keyset pagination returns structured page")
    void testKeysetPagination() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/keyset?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
