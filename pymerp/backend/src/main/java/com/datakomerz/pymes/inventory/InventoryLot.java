package com.datakomerz.pymes.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="inventory_lots")
public class InventoryLot {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="company_id", nullable=false, columnDefinition="uuid") private UUID companyId;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(name="purchase_item_id", columnDefinition="uuid") private UUID purchaseItemId;
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
  public UUID getCompanyId() { return companyId; }
  public void setCompanyId(UUID companyId) { this.companyId = companyId; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public UUID getPurchaseItemId() { return purchaseItemId; }
  public void setPurchaseItemId(UUID purchaseItemId) { this.purchaseItemId = purchaseItemId; }
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