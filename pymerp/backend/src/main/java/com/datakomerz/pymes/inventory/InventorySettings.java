package com.datakomerz.pymes.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_settings")
public class InventorySettings {

  @Id
  @Column(name = "company_id", columnDefinition = "uuid")
  private UUID companyId;

  @Column(name = "low_stock_threshold", nullable = false, precision = 14, scale = 3)
  private BigDecimal lowStockThreshold;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (updatedAt == null) {
      updatedAt = OffsetDateTime.now();
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public void setCompanyId(UUID companyId) {
    this.companyId = companyId;
  }

  public BigDecimal getLowStockThreshold() {
    return lowStockThreshold;
  }

  public void setLowStockThreshold(BigDecimal lowStockThreshold) {
    this.lowStockThreshold = lowStockThreshold;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
