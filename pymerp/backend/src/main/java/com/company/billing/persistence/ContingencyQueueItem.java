package com.company.billing.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contingency_queue_items")
public class ContingencyQueueItem {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false, columnDefinition = "uuid")
  private FiscalDocument document;

  @NotNull
  @Size(max = 100)
  @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
  private String idempotencyKey;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "provider_payload", nullable = false, columnDefinition = "jsonb")
  private JsonNode providerPayload;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private ContingencyQueueStatus status;

  @Column(name = "sync_attempts", nullable = false)
  private int syncAttempts = 0;

  @Column(name = "last_sync_at")
  private OffsetDateTime lastSyncAt;

  @Column(name = "error_detail")
  private String errorDetail;

  @Lob
  @Column(name = "encrypted_blob")
  private byte[] encryptedBlob;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public FiscalDocument getDocument() {
    return document;
  }

  public void setDocument(FiscalDocument document) {
    this.document = document;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public JsonNode getProviderPayload() {
    return providerPayload;
  }

  public void setProviderPayload(JsonNode providerPayload) {
    this.providerPayload = providerPayload;
  }

  public ContingencyQueueStatus getStatus() {
    return status;
  }

  public void setStatus(ContingencyQueueStatus status) {
    this.status = status;
  }

  public int getSyncAttempts() {
    return syncAttempts;
  }

  public void setSyncAttempts(int syncAttempts) {
    this.syncAttempts = syncAttempts;
  }

  public OffsetDateTime getLastSyncAt() {
    return lastSyncAt;
  }

  public void setLastSyncAt(OffsetDateTime lastSyncAt) {
    this.lastSyncAt = lastSyncAt;
  }

  public String getErrorDetail() {
    return errorDetail;
  }

  public void setErrorDetail(String errorDetail) {
    this.errorDetail = errorDetail;
  }

  public byte[] getEncryptedBlob() {
    return encryptedBlob;
  }

  public void setEncryptedBlob(byte[] encryptedBlob) {
    this.encryptedBlob = encryptedBlob;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
