package com.example.payment.util;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// This utility interceptor is kept as a non-bean placeholder to avoid
// conflicting Spring component scanning with the idempotency package
// implementation. Use com.example.payment.idempotency.IdempotencyInterceptor
// for the real implementation.
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Placeholder: check idempotency header and ensure handling (persistence to be implemented later)
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key != null && !key.trim().isEmpty()) {
            // TODO: consult idempotency store
        }
        return true;
    }
}
