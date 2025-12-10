package com.example.payment.api;

import com.example.payment.persistence.SubscriptionEntity;
import com.example.payment.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SubscriptionEntity req,
                                    @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String cid = correlationIdHeader != null ? correlationIdHeader : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            SubscriptionEntity s = subscriptionService.createSubscription(req);
            log.info("Created subscription {} for customer {} correlationId={}", s.getId(), s.getCustomerId(), cid);
            return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + s.getId())).body(java.util.Collections.singletonMap("id", s.getId()));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<SubscriptionEntity> s = subscriptionService.getSubscription(id);
        return s.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renew(@PathVariable String id,
                                   @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String cid = correlationIdHeader != null ? correlationIdHeader : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            subscriptionService.renewSubscription(id);
            log.info("Manually renewed subscription {} correlationId={}", id, cid);
            return ResponseEntity.ok(java.util.Collections.singletonMap("renewedAt", Instant.now().toString()));
        } finally {
            MDC.remove("correlationId");
        }
    }
}
