package com.datakomerz.pymes.purchases;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="purchase_items")
public class PurchaseItem {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="purchase_id", nullable=false, columnDefinition="uuid") private UUID purchaseId;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(nullable=false, precision=14, scale=3) private BigDecimal qty;
  @Column(nullable=false, precision=14, scale=4) private BigDecimal unitCost;
  @Column(precision=5, scale=2) private BigDecimal vatRate;
  @Column(name="mfg_date") private LocalDate mfgDate;
  @Column(name="exp_date") private LocalDate expDate;

  @PrePersist public void pre(){ if(id==null) id=UUID.randomUUID(); }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getPurchaseId() { return purchaseId; }
  public void setPurchaseId(UUID purchaseId) { this.purchaseId = purchaseId; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public BigDecimal getQty() { return qty; }
  public void setQty(BigDecimal qty) { this.qty = qty; }
  public BigDecimal getUnitCost() { return unitCost; }
  public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
  public BigDecimal getVatRate() { return vatRate; }
  public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
  public LocalDate getMfgDate() { return mfgDate; }
  public void setMfgDate(LocalDate mfgDate) { this.mfgDate = mfgDate; }
  public LocalDate getExpDate() { return expDate; }
  public void setExpDate(LocalDate expDate) { this.expDate = expDate; }
}
