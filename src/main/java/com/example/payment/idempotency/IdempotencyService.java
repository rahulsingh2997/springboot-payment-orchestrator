package com.example.payment.idempotency;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    // Placeholder: persist idempotency keys in DB in Phase 2
    public boolean isDuplicate(String key) {
        return false;
    }
}
