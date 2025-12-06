package com.example.payment.audit;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {
    public void record(String userId, String action, String resourceType, String resourceId, String metadata) {
        // placeholder: persist audit log in Phase 2
        System.out.println("AUDIT: " + Instant.now() + " " + action + " " + resourceType + ":" + resourceId);
    }
}
