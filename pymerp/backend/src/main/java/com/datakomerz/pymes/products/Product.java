package com.datakomerz.pymes.products;

import com.datakomerz.pymes.multitenancy.TenantFiltered;
import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id","sku"}))
@TenantFiltered
@Schema(name = "Product", description = "Producto del catálogo de la empresa")
public class Product extends TenantAwareEntity {

  @Id
  @Column(columnDefinition = "uuid")
  @Schema(description = "ID del producto", example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID id;

  @Column(nullable = false, length = 32)
  @Schema(description = "Código interno o SKU", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
  private String sku;

  @Column(nullable = false, length = 120)
  @Schema(description = "Nombre visible del producto", example = "Laptop Dell XPS 15", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "Descripción breve del producto", example = "Ultrabook 15\" con Intel Core i7 y 16 GB RAM")
  private String description;
  @Schema(description = "Categoría o familia", example = "Tecnología")
  private String category;

  @Column(length = 64)
  @Schema(description = "Código de barras (EAN/UPC)", example = "7801234567890")
  private String barcode;

  @Column(name = "image_url")
  @Schema(description = "URL de la imagen principal", example = "https://cdn.pymerp.cl/products/prod-001.png")
  private String imageUrl;

  @Column(name = "qr_url")
  @Schema(description = "URL del código QR generado para etiquetado", example = "https://cdn.pymerp.cl/products/prod-001-qr.png")
  private String qrUrl;

  @Column(name = "critical_stock", nullable = false)
  @Schema(description = "Stock mínimo antes de generar alerta", example = "10")
  private BigDecimal criticalStock = BigDecimal.ZERO;

  @Column(name = "is_active", nullable = false)
  @Schema(description = "Indica si el producto está disponible para la venta", example = "true")
  private Boolean active;

  @Column(name = "created_at")
  @Schema(description = "Fecha de creación", example = "2024-01-15T12:30:00Z")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @Schema(description = "Última fecha de actualización", example = "2024-02-10T09:45:00Z")
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  @Schema(description = "Fecha de eliminación lógica (si aplica)", example = "2024-03-01T00:00:00Z")
  private OffsetDateTime deletedAt;

  @Schema(description = "Número de versión del registro", example = "3")
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
