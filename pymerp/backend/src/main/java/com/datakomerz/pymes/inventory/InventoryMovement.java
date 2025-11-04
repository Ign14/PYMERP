package com.datakomerz.pymes.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="inventory_movements")
public class InventoryMovement {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="company_id", nullable=false, columnDefinition="uuid") private UUID companyId;
  @Column(name="product_id", nullable=false, columnDefinition="uuid") private UUID productId;
  @Column(name="lot_id", columnDefinition="uuid") private UUID lotId;
  @Column(nullable=false, length=20) private String type;
  @Column(nullable=false, precision=14, scale=3) private BigDecimal qty;
  private String refType;
  @Column(columnDefinition="uuid") private UUID refId;
  @Column(name="note") private String note;
  
  // Campos de auditoría y trazabilidad
  @Column(name="created_by") private String createdBy;
  @Column(name="user_ip") private String userIp;
  @Column(name="reason_code") private String reasonCode;
  @Column(name="previous_qty") private BigDecimal previousQty;
  @Column(name="new_qty") private BigDecimal newQty;
  
  @Column(name="created_at") private OffsetDateTime createdAt;

  @PrePersist public void pre(){
    if(id==null) id=UUID.randomUUID();
    if(createdAt==null) createdAt=OffsetDateTime.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getCompanyId() { return companyId; }
  public void setCompanyId(UUID companyId) { this.companyId = companyId; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public UUID getLotId() { return lotId; }
  public void setLotId(UUID lotId) { this.lotId = lotId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getQty() { return qty; }
  public void setQty(BigDecimal qty) { this.qty = qty; }
  public String getRefType() { return refType; }
  public void setRefType(String refType) { this.refType = refType; }
  public UUID getRefId() { return refId; }
  public void setRefId(UUID refId) { this.refId = refId; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public String getCreatedBy() { return createdBy; }
  public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
  public String getUserIp() { return userIp; }
  public void setUserIp(String userIp) { this.userIp = userIp; }
  public String getReasonCode() { return reasonCode; }
  public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
  public BigDecimal getPreviousQty() { return previousQty; }
  public void setPreviousQty(BigDecimal previousQty) { this.previousQty = previousQty; }
  public BigDecimal getNewQty() { return newQty; }
  public void setNewQty(BigDecimal newQty) { this.newQty = newQty; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
