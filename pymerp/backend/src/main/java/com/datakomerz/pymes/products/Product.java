package com.datakomerz.pymes.products;

import com.datakomerz.pymes.multitenancy.TenantFiltered;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id","sku"}))
@TenantFiltered
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = org.hibernate.type.descriptor.java.UUIDJavaType.class))
@Filter(name = "tenantFilter", condition = "company_id = :tenantId")
public class Product {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
  private UUID companyId;

  @Column(nullable = false, length = 32)
  private String sku;

  @Column(nullable = false, length = 120)
  private String name;

  private String description;
  private String category;

  @Column(length = 64)
  private String barcode;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "qr_url")
  private String qrUrl;

  @Column(name = "critical_stock", nullable = false)
  private BigDecimal criticalStock = BigDecimal.ZERO;

  @Column(name = "is_active", nullable = false)
  private Boolean active;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  private Integer version;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
    if (version == null) {
      version = 0;
    }
    if (active == null) {
      active = Boolean.TRUE;
    }
    if (criticalStock == null) {
      criticalStock = BigDecimal.ZERO;
    }
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = OffsetDateTime.now();
    this.version = (this.version == null ? 0 : this.version + 1);
    if (active == null) {
      active = Boolean.TRUE;
    }
    if (criticalStock == null) {
      criticalStock = BigDecimal.ZERO;
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public void setCompanyId(UUID companyId) {
    this.companyId = companyId;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }

  public BigDecimal getCriticalStock() {
    return criticalStock;
  }

  public void setCriticalStock(BigDecimal criticalStock) {
    this.criticalStock = criticalStock;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(OffsetDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
