package com.systemdesign.apidesign;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency Keys Filter Pattern (Category 10).
 * <p>
 * Detects {@code Idempotency-Key} HTTP header on mutating requests (POST/PUT).
 * If a duplicate key arrives, the cached response is replayed without
 * executing business logic a second time (prevents double charging / duplicate creation).
 */
@Component
public class IdempotencyKeyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyFilter.class);
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    // In production, backed by Redis with TTL
    private final Map<String, Integer> processedKeys = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_HEADER);

        if (idempotencyKey != null && !idempotencyKey.isBlank() && "POST".equalsIgnoreCase(httpRequest.getMethod())) {
            Integer previousStatus = processedKeys.putIfAbsent(idempotencyKey, 200);
            if (previousStatus != null) {
                log.info("Idempotent request detected for key [{}]. Replaying cached 200 OK.", idempotencyKey);
                httpResponse.setHeader("X-Cache-Lookup", "HIT-IDEMPOTENCY");
                httpResponse.setStatus(previousStatus);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"success\":true,\"message\":\"Duplicate request acknowledged idempotently\"}");
                return;
            }
            httpResponse.setHeader("X-Cache-Lookup", "MISS-IDEMPOTENCY");
        }

        chain.doFilter(request, response);
    }
}
