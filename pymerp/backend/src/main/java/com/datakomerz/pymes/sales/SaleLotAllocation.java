package com.datakomerz.pymes.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="sale_lot_allocations")
public class SaleLotAllocation {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="sale_id", nullable=false, columnDefinition="uuid") private UUID saleId;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(name="lot_id", nullable=false, columnDefinition="uuid") private UUID lotId;
  @Column(nullable=false, precision=14, scale=3) private BigDecimal qty;

  @PrePersist public void pre(){ if(id==null) id=UUID.randomUUID(); }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getSaleId() { return saleId; }
  public void setSaleId(UUID saleId) { this.saleId = saleId; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public UUID getLotId() { return lotId; }
  public void setLotId(UUID lotId) { this.lotId = lotId; }
  public BigDecimal getQty() { return qty; }
  public void setQty(BigDecimal qty) { this.qty = qty; }
}