package com.datakomerz.pymes.billing.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBillingIdempotencyStore implements BillingIdempotencyStore {

  private record Entry(IdempotencyEntry data, Instant expiresAt) {}

  private final Map<UUID, Map<String, Entry>> store = new ConcurrentHashMap<>();
  private final Duration ttl = Duration.ofMinutes(10);

  @Override
  public Optional<IdempotencyEntry> findEntry(UUID tenantId, String idempotencyKey) {
    cleanup(tenantId, idempotencyKey);
    return Optional.ofNullable(store.getOrDefault(tenantId, Map.of()).get(idempotencyKey))
      .map(Entry::data);
  }

  @Override
  public boolean reserve(UUID tenantId, String idempotencyKey, String payloadHash) {
    Entry entry = new Entry(new IdempotencyEntry(null, payloadHash), Instant.now().plus(ttl));
    Entry previous = store.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>())
      .putIfAbsent(idempotencyKey, entry);
    return previous == null;
  }

  @Override
  public Optional<IdempotencyEntry> awaitCompletion(UUID tenantId, String idempotencyKey) {
    return findEntry(tenantId, idempotencyKey);
  }

  @Override
  public void complete(UUID tenantId, String idempotencyKey, String payloadHash, UUID documentId) {
    store.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>())
      .put(idempotencyKey, new Entry(new IdempotencyEntry(documentId, payloadHash), Instant.now().plus(ttl)));
  }

  @Override
  public void invalidate(UUID tenantId, String idempotencyKey) {
    Map<String, Entry> tenantEntries = store.get(tenantId);
    if (tenantEntries != null) {
      tenantEntries.remove(idempotencyKey);
    }
  }

  private void cleanup(UUID tenantId, String key) {
    Map<String, Entry> tenantEntries = store.get(tenantId);
    if (tenantEntries == null) {
      return;
    }
    Entry entry = tenantEntries.get(key);
    if (entry != null && entry.expiresAt().isBefore(Instant.now())) {
      tenantEntries.remove(key);
    }
  }
}
