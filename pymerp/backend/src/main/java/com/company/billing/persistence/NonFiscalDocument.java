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
@Table(name = "non_fiscal_documents")
public class NonFiscalDocument {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false, columnDefinition = "uuid")
  private Sale sale;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 32)
  private NonFiscalDocumentType documentType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private NonFiscalDocumentStatus status = NonFiscalDocumentStatus.READY;

  @Size(max = 40)
  @Column(length = 40)
  private String number;

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

  public NonFiscalDocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(NonFiscalDocumentType documentType) {
    this.documentType = documentType;
  }

  public NonFiscalDocumentStatus getStatus() {
    return status;
  }

  public void setStatus(NonFiscalDocumentStatus status) {
    this.status = status;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
