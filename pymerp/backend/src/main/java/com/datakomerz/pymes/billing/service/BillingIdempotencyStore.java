package com.datakomerz.pymes.billing.service;

import java.util.Optional;
import java.util.UUID;

public interface BillingIdempotencyStore {

  record IdempotencyEntry(UUID documentId, String payloadHash) {}

  Optional<IdempotencyEntry> findEntry(UUID tenantId, String idempotencyKey);

  boolean reserve(UUID tenantId, String idempotencyKey, String payloadHash);

  Optional<IdempotencyEntry> awaitCompletion(UUID tenantId, String idempotencyKey);

  void complete(UUID tenantId, String idempotencyKey, String payloadHash, UUID documentId);

  void invalidate(UUID tenantId, String idempotencyKey);
}
