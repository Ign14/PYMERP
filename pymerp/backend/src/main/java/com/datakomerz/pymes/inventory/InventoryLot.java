package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="inventory_lots")
@TenantFiltered
@Schema(name = "InventoryLot", description = "Lote de inventario disponible para un producto")
public class InventoryLot extends TenantAwareEntity {
  @Id
  @Column(columnDefinition="uuid")
  @Schema(description = "ID del lote", example = "b1a7199f-6ab0-4f72-b8c3-5fdc63fb7e32")
  private UUID id;

  @Column(name="product_id", nullable=false, columnDefinition="uuid")
  @Schema(description = "ID del producto asociado", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
  private UUID productId;

  @Column(name="purchase_item_id", columnDefinition="uuid")
  @Schema(description = "Detalle de compra que originó el lote", example = "2aeb4d3e-0af6-4f5e-9b61-1b5b504e5ed7")
  private UUID purchaseItemId;

  @Column(name="location_id", columnDefinition="uuid")
  @Schema(description = "Ubicación o bodega", example = "7f92b2d7-8f21-4ad2-bff8-119c5b95abc1")
  private UUID locationId;

  @Column(name="qty_available", nullable=false, precision=14, scale=3)
  @Schema(description = "Cantidad disponible", example = "125.500")
  private BigDecimal qtyAvailable;

  @Column(name="cost_unit", precision=14, scale=4)
  @Schema(description = "Costo unitario del lote", example = "845.1234")
  private BigDecimal costUnit;

  @Schema(description = "Fecha de fabricación", example = "2024-01-10")
  private LocalDate mfgDate;

  @Schema(description = "Fecha de expiración", example = "2025-01-10")
  private LocalDate expDate;

  @Column(name="created_at")
  @Schema(description = "Fecha de creación del lote", example = "2024-01-12T08:00:00Z")
  private OffsetDateTime createdAt;

  @PrePersist public void pre(){
    if(id==null) id=UUID.randomUUID();
    if(createdAt==null) createdAt=OffsetDateTime.now();
  }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public UUID getPurchaseItemId() { return purchaseItemId; }
  public void setPurchaseItemId(UUID purchaseItemId) { this.purchaseItemId = purchaseItemId; }
  public UUID getLocationId() { return locationId; }
  public void setLocationId(UUID locationId) { this.locationId = locationId; }
  public BigDecimal getQtyAvailable() { return qtyAvailable; }
  public void setQtyAvailable(BigDecimal qtyAvailable) { this.qtyAvailable = qtyAvailable; }
  public BigDecimal getCostUnit() { return costUnit; }
  public void setCostUnit(BigDecimal costUnit) { this.costUnit = costUnit; }
  public LocalDate getMfgDate() { return mfgDate; }
  public void setMfgDate(LocalDate mfgDate) { this.mfgDate = mfgDate; }
  public LocalDate getExpDate() { return expDate; }
  public void setExpDate(LocalDate expDate) { this.expDate = expDate; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
