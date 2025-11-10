package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.multitenancy.TenantFiltered;
import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="inventory_lots")
@TenantFiltered
public class InventoryLot extends TenantAwareEntity {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(name="purchase_item_id", columnDefinition="uuid") private UUID purchaseItemId;
  @Column(name="purchase_id", columnDefinition="uuid") private UUID purchaseId;
  @Column(name="location_id", columnDefinition="uuid") private UUID locationId;
  @Column(name="batch_name", length=100) private String batchName;
  @Column(name="qty_available", nullable=false, precision=14, scale=3) private BigDecimal qtyAvailable;
  @Column(name="cost_unit", precision=14, scale=4) private BigDecimal costUnit;
  private LocalDate mfgDate;
  private LocalDate expDate;
  @Column(name="created_at") private OffsetDateTime createdAt;

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
  public UUID getPurchaseId() { return purchaseId; }
  public void setPurchaseId(UUID purchaseId) { this.purchaseId = purchaseId; }
  public UUID getLocationId() { return locationId; }
  public void setLocationId(UUID locationId) { this.locationId = locationId; }
  public String getBatchName() { return batchName; }
  public void setBatchName(String batchName) { this.batchName = batchName; }
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