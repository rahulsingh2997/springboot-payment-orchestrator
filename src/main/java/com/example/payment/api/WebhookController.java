package com.example.payment.api;

import com.example.payment.service.WebhookService;
import com.example.payment.webhook.WebhookSignatureVerifier;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookSignatureVerifier verifier;
    private final WebhookService webhookService;

    public WebhookController(WebhookSignatureVerifier verifier, WebhookService webhookService) {
        this.verifier = verifier;
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestBody String payload,
            @RequestHeader(value = "X-ANET-Signature", required = false) String signature,
            @RequestHeader(value = "X-Source", required = false) String source,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        if (correlationId == null || correlationId.isEmpty()) correlationId = java.util.UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            boolean ok = verifier.verify(payload, signature);
            if (!ok) {
                org.slf4j.LoggerFactory.getLogger(WebhookController.class).warn("Invalid webhook signature for correlationId={}", correlationId);
                Map<String,Object> body = new HashMap<>();
                body.put("timestamp", java.time.Instant.now().toString());
                body.put("status", HttpStatus.UNAUTHORIZED.value());
                body.put("error", "Invalid signature");
                body.put("correlationId", correlationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            org.slf4j.LoggerFactory.getLogger(WebhookController.class).info("Webhook signature verified for correlationId={}", correlationId);

            String id = webhookService.handleWebhook(payload, source, signature, correlationId);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/api/v1/webhooks/" + id));
            Map<String,Object> body = new HashMap<>();
            body.put("id", id);
            body.put("status", "accepted");
            body.put("correlationId", correlationId);
            return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
