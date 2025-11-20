package com.datakomerz.pymes.billing.service;

import com.datakomerz.pymes.billing.config.BillingIdempotencyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@Primary
public class RedisBillingIdempotencyStore implements BillingIdempotencyStore {

  private static final Logger log = LoggerFactory.getLogger(RedisBillingIdempotencyStore.class);

  private final StringRedisTemplate redisTemplate;
  private final BillingIdempotencyProperties properties;
  private final ObjectMapper objectMapper;

  public RedisBillingIdempotencyStore(StringRedisTemplate redisTemplate,
                                      BillingIdempotencyProperties properties,
                                      ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<IdempotencyEntry> findEntry(UUID tenantId, String idempotencyKey) {
    String key = redisKey(tenantId, idempotencyKey);
    String payload = redisTemplate.opsForValue().get(key);
    if (payload == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(payload, IdempotencyEntry.class));
    } catch (JsonProcessingException ex) {
      log.warn("Unable to deserialize idempotency entry for key {}: {}", key, ex.getMessage());
      redisTemplate.delete(key);
      return Optional.empty();
    }
  }

  @Override
  public boolean reserve(UUID tenantId, String idempotencyKey, String payloadHash) {
    String key = redisKey(tenantId, idempotencyKey);
    String serialized = serialize(new IdempotencyEntry(null, payloadHash));
    if (serialized == null) {
      return false;
    }
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(key, serialized, properties.getTtl());
    return Boolean.TRUE.equals(success);
  }

  @Override
  public Optional<IdempotencyEntry> awaitCompletion(UUID tenantId, String idempotencyKey) {
    Duration timeout = properties.getWaitTimeout();
    Duration poll = properties.getWaitPollInterval();
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      Optional<IdempotencyEntry> entry = findEntry(tenantId, idempotencyKey);
      if (entry.isPresent()) {
        if (entry.get().documentId() != null) {
          return entry;
        }
      } else {
        return Optional.empty();
      }
      try {
        Thread.sleep(Math.max(1, poll.toMillis()));
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  @Override
  public void complete(UUID tenantId, String idempotencyKey, String payloadHash, UUID documentId) {
    String key = redisKey(tenantId, idempotencyKey);
    String payload = serialize(new IdempotencyEntry(documentId, payloadHash));
    if (payload != null) {
      redisTemplate.opsForValue().set(key, payload, properties.getTtl());
    }
  }

  @Override
  public void invalidate(UUID tenantId, String idempotencyKey) {
    redisTemplate.delete(redisKey(tenantId, idempotencyKey));
  }

  private String redisKey(UUID tenantId, String idempotencyKey) {
    return "billing:idempotency:" + tenantId + ":" + idempotencyKey;
  }

  private String serialize(IdempotencyEntry entry) {
    try {
      return objectMapper.writeValueAsString(entry);
    } catch (JsonProcessingException ex) {
      log.warn("Unable to serialize idempotency entry: {}", ex.getMessage());
      return null;
    }
  }
}
