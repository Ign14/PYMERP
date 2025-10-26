package com.company.billing.persistence;

import com.datakomerz.pymes.sales.Sale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "fiscal_documents")
public class FiscalDocument {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false, columnDefinition = "uuid")
  private Sale sale;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 16)
  private FiscalDocumentType documentType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "tax_mode", nullable = false, length = 16)
  private TaxMode taxMode;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private FiscalDocumentStatus status;

  @Size(max = 30)
  @Column(length = 30)
  private String number;

  @Size(max = 40)
  @Column(name = "provisional_number", length = 40, unique = true)
  private String provisionalNumber;

  @Size(max = 160)
  @Column(name = "contingency_reason", length = 160)
  private String contingencyReason;

  @Size(max = 100)
  @Column(name = "idempotency_key", length = 100, unique = true)
  private String idempotencyKey;

  @Size(max = 80)
  @Column(name = "track_id", length = 80)
  private String trackId;

  @Size(max = 60)
  @Column(length = 60)
  private String provider;

  @NotNull
  @Column(name = "is_offline", nullable = false)
  private boolean offline = false;

  @Column(name = "sync_attempts", nullable = false)
  private int syncAttempts = 0;

  @Column(name = "last_sync_at")
  private OffsetDateTime lastSyncAt;

  @Column(name = "error_detail")
  private String errorDetail;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
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

  public Sale getSale() {
    return sale;
  }

  public void setSale(Sale sale) {
    this.sale = sale;
  }

  public FiscalDocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(FiscalDocumentType documentType) {
    this.documentType = documentType;
  }

  public TaxMode getTaxMode() {
    return taxMode;
  }

  public void setTaxMode(TaxMode taxMode) {
    this.taxMode = taxMode;
  }

  public FiscalDocumentStatus getStatus() {
    return status;
  }

  public void setStatus(FiscalDocumentStatus status) {
    this.status = status;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getProvisionalNumber() {
    return provisionalNumber;
  }

  public void setProvisionalNumber(String provisionalNumber) {
    this.provisionalNumber = provisionalNumber;
  }

  public String getContingencyReason() {
    return contingencyReason;
  }

  public void setContingencyReason(String contingencyReason) {
    this.contingencyReason = contingencyReason;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getTrackId() {
    return trackId;
  }

  public void setTrackId(String trackId) {
    this.trackId = trackId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public boolean isOffline() {
    return offline;
  }

  public void setOffline(boolean offline) {
    this.offline = offline;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
