package com.datakomerz.pymes.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="sale_items")
public class SaleItem {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="sale_id", nullable=false, columnDefinition="uuid") private UUID saleId;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(nullable=false, precision=14, scale=3) private BigDecimal qty;
  @Column(nullable=false, precision=14, scale=4) private BigDecimal unitPrice;
  @Column(nullable=false, precision=14, scale=4) private BigDecimal discount;

  @PrePersist public void pre(){ if(id==null) id=UUID.randomUUID(); if(discount==null) discount = BigDecimal.ZERO; }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getSaleId() { return saleId; }
  public void setSaleId(UUID saleId) { this.saleId = saleId; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public BigDecimal getQty() { return qty; }
  public void setQty(BigDecimal qty) { this.qty = qty; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public BigDecimal getDiscount() { return discount; }
  public void setDiscount(BigDecimal discount) { this.discount = discount; }
}