package com.datakomerz.pymes.billing.job;

import com.company.billing.persistence.ContingencyQueueItem;
import com.company.billing.persistence.ContingencyQueueItemRepository;
import com.company.billing.persistence.ContingencyQueueStatus;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.datakomerz.pymes.billing.config.BillingOfflineProperties;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.provider.BillingProvider;
import com.datakomerz.pymes.billing.provider.BillingProvider.IssueInvoiceResult;
import com.datakomerz.pymes.billing.provider.BillingProviderException;
import com.datakomerz.pymes.billing.provider.BillingProviderTransientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ContingencySyncJob {

  private static final Logger log = LoggerFactory.getLogger(ContingencySyncJob.class);
  private static final List<ContingencyQueueStatus> CANDIDATE_STATUSES = List.of(
      ContingencyQueueStatus.OFFLINE_PENDING,
      ContingencyQueueStatus.SYNCING
  );
  private static final int MAX_ERROR_DETAIL_LENGTH = 255;

  private final BillingOfflineProperties properties;
  private final ContingencyQueueItemRepository queueRepository;
  private final FiscalDocumentRepository fiscalDocumentRepository;
  private final Optional<BillingProvider> billingProvider;
  private final Clock clock;
  private final AtomicInteger pendingGauge;
  private final AtomicInteger syncingGauge;
  private final Counter failureCounter;
  private final DistributionSummary latencySummary;

  public ContingencySyncJob(BillingOfflineProperties properties,
                            ContingencyQueueItemRepository queueRepository,
                            FiscalDocumentRepository fiscalDocumentRepository,
                            ObjectProvider<BillingProvider> billingProviderProvider,
                            ObjectProvider<Clock> clockProvider,
                            MeterRegistry meterRegistry) {
    this.properties = properties;
    this.queueRepository = queueRepository;
    this.fiscalDocumentRepository = fiscalDocumentRepository;
    this.billingProvider = Optional.ofNullable(billingProviderProvider.getIfAvailable());
    Clock providedClock = clockProvider.getIfAvailable();
    this.clock = providedClock != null ? providedClock : Clock.systemUTC();
    this.pendingGauge = meterRegistry.gauge("billing.contingency.pending", new AtomicInteger(0));
    this.syncingGauge = meterRegistry.gauge("billing.contingency.syncing", new AtomicInteger(0));
    this.failureCounter = Counter.builder("billing.contingency.failures")
        .description("Total contingency sync failures")
        .register(meterRegistry);
    this.latencySummary = DistributionSummary.builder("billing.contingency.latencyMs")
        .description("Latency in milliseconds from offline enqueue to successful sync")
        .baseUnit("milliseconds")
        .register(meterRegistry);
  }

  @Scheduled(
      initialDelayString = "${billing.offline.retry.backoffMs:30000}",
      fixedDelayString = "${billing.offline.retry.backoffMs:30000}")
  public void run() {
    processQueue();
  }

  @Transactional
  public void processQueue() {
    try {
      if (!properties.isEnabled()) {
        return;
      }
      BillingProvider provider = billingProvider.orElse(null);
      if (provider == null) {
        log.debug("Skipping contingency sync because billing provider is not configured");
        return;
      }
      List<ContingencyQueueItem> candidates = queueRepository
          .findTop20ByStatusInOrderByCreatedAtAsc(CANDIDATE_STATUSES);
      if (candidates.isEmpty()) {
        return;
      }
      for (ContingencyQueueItem item : candidates) {
        if (item.getDocument() == null) {
          log.warn("Skipping contingency item without linked document idempotencyKey={}",
              item.getIdempotencyKey());
          continue;
        }
        if (isAlreadySynced(item.getDocument())) {
          continue;
        }
        if (item.getSyncAttempts() >= properties.getRetry().getMaxAttempts()) {
          markAsFailed(item, item.getDocument(), "Max retry attempts exceeded");
          continue;
        }
        if (!isReadyForRetry(item)) {
          continue;
        }
        attemptSync(item, provider);
      }
    } catch (Exception ex) {
      log.error("Unexpected error while running contingency sync job", ex);
    } finally {
      updateMetrics();
    }
  }

  private void attemptSync(ContingencyQueueItem queueItem, BillingProvider provider) {
    OffsetDateTime attemptTime = now();
    int attempts = queueItem.getSyncAttempts() + 1;
    queueItem.setStatus(ContingencyQueueStatus.SYNCING);
    queueItem.setSyncAttempts(attempts);
    queueItem.setLastSyncAt(attemptTime);
    queueItem.setErrorDetail(null);

    FiscalDocument document = queueItem.getDocument();
    document.setStatus(FiscalDocumentStatus.SYNCING);
    document.setSyncAttempts(attempts);
    document.setLastSyncAt(attemptTime);
    document.setErrorDetail(null);
    document.setOffline(true);

    UUID documentId = document.getId();
    String idempotencyKey = queueItem.getIdempotencyKey();

    try {
      InvoicePayload payload = buildInvoicePayload(queueItem);
      IssueInvoiceResult result = provider.issueInvoice(payload, idempotencyKey);
      handleSuccess(queueItem, document, result, attemptTime);
      log.info("Contingency sync succeeded for documentId={} idempotencyKey={} attempts={} trackId={}",
          documentId, idempotencyKey, attempts, result.trackId());
    } catch (BillingProviderTransientException ex) {
      handleTransientFailure(queueItem, document, ex, attemptTime);
      log.warn("Transient contingency sync failure for documentId={} idempotencyKey={} attempts={}: {}",
          documentId, idempotencyKey, attempts, ex.getMessage(), ex);
    } catch (BillingProviderException ex) {
      handlePermanentFailure(queueItem, document, ex, attemptTime);
      log.error("Permanent contingency sync failure for documentId={} idempotencyKey={} attempts={}: {}",
          documentId, idempotencyKey, attempts, ex.getMessage(), ex);
    } catch (Exception ex) {
      handleTransientFailure(queueItem, document, ex, attemptTime);
      log.warn("Unexpected contingency sync error for documentId={} idempotencyKey={} attempts={}: {}",
          documentId, idempotencyKey, attempts, ex.getMessage(), ex);
    }
  }

  private InvoicePayload buildInvoicePayload(ContingencyQueueItem queueItem) {
    FiscalDocument document = queueItem.getDocument();
    ObjectNode payloadNode = asObjectNode(queueItem.getProviderPayload());
    JsonNode saleNode = payloadNode.path("sale");
    String deviceId = readText(saleNode, "deviceId");
    String pointOfSale = readText(saleNode, "pointOfSale");
    UUID saleId = document.getSale() != null ? document.getSale().getId() : null;
    return new InvoicePayload(
        saleId,
        document.getDocumentType(),
        null,
        document.getTaxMode(),
        document.getSiiDocumentType(),
        deviceId,
        pointOfSale,
        payloadNode);
  }

  private void handleSuccess(ContingencyQueueItem queueItem,
                             FiscalDocument document,
                             IssueInvoiceResult result,
                             OffsetDateTime attemptTime) {
    document.setStatus(FiscalDocumentStatus.SENT);
    document.setOffline(false);
    document.setProvider(result.provider());
    document.setTrackId(result.trackId());
    document.setNumber(result.number());
    document.setFinalFolio(result.number());
    document.setErrorDetail(null);
    document.setLastSyncAt(attemptTime);

    OffsetDateTime createdAt = queueItem.getCreatedAt();
    if (createdAt != null) {
      long latency = attemptTime.toInstant().toEpochMilli() - createdAt.toInstant().toEpochMilli();
      if (latency >= 0) {
        latencySummary.record(Math.max(latency, 1));
      }
    }

    fiscalDocumentRepository.save(document);
    // FIX: remove successfully synced contingency items to prevent queue lock.
    queueRepository.delete(queueItem);
  }

  private void handleTransientFailure(ContingencyQueueItem queueItem,
                                      FiscalDocument document,
                                      Exception ex,
                                      OffsetDateTime attemptTime) {
    String detail = truncate(ex.getMessage());
    queueItem.setStatus(ContingencyQueueStatus.OFFLINE_PENDING);
    queueItem.setErrorDetail(detail);
    queueItem.setLastSyncAt(attemptTime);

    document.setStatus(FiscalDocumentStatus.OFFLINE_PENDING);
    document.setOffline(true);
    document.setErrorDetail(detail);
    document.setLastSyncAt(attemptTime);

    fiscalDocumentRepository.save(document);
    queueRepository.save(queueItem);
  }

  private void handlePermanentFailure(ContingencyQueueItem queueItem,
                                      FiscalDocument document,
                                      Exception ex,
                                      OffsetDateTime attemptTime) {
    String detail = truncate(ex.getMessage());
    failureCounter.increment();

    queueItem.setStatus(ContingencyQueueStatus.FAILED);
    queueItem.setErrorDetail(detail);
    queueItem.setLastSyncAt(attemptTime);

    document.setStatus(FiscalDocumentStatus.FAILED);
    document.setOffline(false);
    document.setErrorDetail(detail);
    document.setLastSyncAt(attemptTime);

    fiscalDocumentRepository.save(document);
    queueRepository.save(queueItem);
  }

  private void markAsFailed(ContingencyQueueItem queueItem,
                            FiscalDocument document,
                            String reason) {
    OffsetDateTime now = now();
    String detail = truncate(reason);
    failureCounter.increment();
    queueItem.setStatus(ContingencyQueueStatus.FAILED);
    queueItem.setErrorDetail(detail);
    queueItem.setLastSyncAt(now);

    document.setStatus(FiscalDocumentStatus.FAILED);
    document.setOffline(false);
    document.setErrorDetail(detail);
    document.setLastSyncAt(now);

    fiscalDocumentRepository.save(document);
    queueRepository.save(queueItem);
    log.error("Contingency item failed permanently due to retries exhausted documentId={} idempotencyKey={}",
        document.getId(), queueItem.getIdempotencyKey());
  }

  private boolean isReadyForRetry(ContingencyQueueItem item) {
    Instant nowInstant = clock.instant();
    OffsetDateTime lastSyncAt = item.getLastSyncAt();
    if (lastSyncAt == null) {
      return true;
    }
    long baseDelay = properties.getRetry().getBackoffMs();
    if (baseDelay <= 0) {
      return true;
    }
    int attempts = Math.max(item.getSyncAttempts(), 1);
    int exponent = Math.min(attempts - 1, 20);
    long multiplier = 1L << Math.max(exponent, 0);
    long delay;
    if (baseDelay > Long.MAX_VALUE / multiplier) {
      delay = Long.MAX_VALUE;
    } else {
      delay = baseDelay * multiplier;
    }
    Instant nextAttempt = lastSyncAt.toInstant().plusMillis(delay);
    return !nowInstant.isBefore(nextAttempt);
  }

  private boolean isAlreadySynced(FiscalDocument document) {
    FiscalDocumentStatus status = document.getStatus();
    return status == FiscalDocumentStatus.SENT || status == FiscalDocumentStatus.ACCEPTED;
  }

  private void updateMetrics() {
    if (pendingGauge != null) {
      pendingGauge.set((int) queueRepository.countByStatus(ContingencyQueueStatus.OFFLINE_PENDING));
    }
    if (syncingGauge != null) {
      syncingGauge.set((int) queueRepository.countByStatus(ContingencyQueueStatus.SYNCING));
    }
  }

  private ObjectNode asObjectNode(JsonNode node) {
    if (node instanceof ObjectNode objectNode) {
      return objectNode.deepCopy();
    }
    return JsonNodeFactory.instance.objectNode();
  }

  private String readText(JsonNode node, String field) {
    if (node == null) {
      return null;
    }
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private String truncate(String message) {
    if (message == null) {
      return null;
    }
    String trimmed = message.trim();
    return trimmed.length() > MAX_ERROR_DETAIL_LENGTH
        ? trimmed.substring(0, MAX_ERROR_DETAIL_LENGTH)
        : trimmed;
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(clock);
  }
}
