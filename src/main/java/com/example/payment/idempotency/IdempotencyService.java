package com.example.payment.idempotency;

import com.example.payment.persistence.IdempotencyKeyEntity;
import com.example.payment.persistence.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public Optional<IdempotencyKeyEntity> find(String key) {
        return repository.findById(key);
    }

    public static class ClaimResult {
        public enum Type { NEW, REPLAY, CONFLICT }
        private final Type type;
        private final IdempotencyKeyEntity entity;

        public ClaimResult(Type type, IdempotencyKeyEntity entity) {
            this.type = type;
            this.entity = entity;
        }

        public Type getType() { return type; }
        public IdempotencyKeyEntity getEntity() { return entity; }
    }

    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String message) { super(message); }
    }

    @Transactional
    public IdempotencyKeyEntity createIfAbsent(String key, String requestHash) {
        Optional<IdempotencyKeyEntity> existing = repository.findById(key);
        if (existing.isPresent()) return existing.get();

        IdempotencyKeyEntity ent = new IdempotencyKeyEntity();
        ent.setKey(key);
        ent.setRequestHash(requestHash);
        ent.setCreatedAt(Instant.now());
        ent.setResponseBody(null);
        return repository.save(ent);
    }

    /**
     * Claim an idempotency key for processing or return an existing snapshot/conflict.
     * - NEW: key claimed (created or existing without response and matching hash)
     * - REPLAY: existing response snapshot available (matching hash)
     * - CONFLICT: existing request hash differs -> conflict
     */
    @Transactional
    public ClaimResult claim(String key, String requestHash) {
        Optional<IdempotencyKeyEntity> existing = repository.findById(key);
        if (existing.isPresent()) {
            IdempotencyKeyEntity e = existing.get();
            // If we already have a saved response snapshot, return REPLAY first
            // (this ensures repeated requests with the same key return the same
            // response even if minor hash differences occur due to whitespace/encoding)
            if (e.getResponseBody() != null) {
                return new ClaimResult(ClaimResult.Type.REPLAY, e);
            }
            String storedHash = e.getRequestHash();
            if (storedHash != null && !storedHash.equals(requestHash)) {
                return new ClaimResult(ClaimResult.Type.CONFLICT, e);
            }
            // existing claim but no response yet -> allow processing (NEW)
            return new ClaimResult(ClaimResult.Type.NEW, e);
        }

        IdempotencyKeyEntity ent = new IdempotencyKeyEntity();
        ent.setKey(key);
        ent.setRequestHash(requestHash);
        ent.setCreatedAt(Instant.now());
        ent.setResponseBody(null);
        repository.save(ent);
        return new ClaimResult(ClaimResult.Type.NEW, ent);
    }

    @Transactional
    public void saveResponse(String key, String responseBody) {
        repository.findById(key).ifPresent(e -> {
            e.setResponseBody(responseBody);
            repository.save(e);
        });
    }

    @Transactional
    public void saveResponse(String key, int responseStatus, String responseHeadersJson, String responseBody) {
        repository.findById(key).ifPresent(e -> {
            e.setResponseStatus(responseStatus);
            e.setResponseHeaders(responseHeadersJson);
            e.setResponseBody(responseBody);
            e.setConsumedAt(Instant.now());
            e.setStatus("COMPLETED");
            repository.save(e);
        });
    }

    // Backward-compatible helper used by the existing interceptor placeholder.
    // Returns false to let the filter handle idempotency semantics;
    // keep this to avoid runtime NoSuchMethod errors from older code paths.
    public boolean isDuplicate(String key) {
        return false;
    }
}
