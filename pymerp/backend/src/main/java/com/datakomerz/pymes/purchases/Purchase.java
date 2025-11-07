package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchases", indexes = {
  @Index(name = "idx_purchases_company", columnList = "company_id")
})
@TenantFiltered
public class Purchase extends TenantAwareEntity {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="supplier_id", columnDefinition="uuid") private UUID supplierId;
  private String docType;
  private String docNumber;
  private String status;
  private BigDecimal net;
  private BigDecimal vat;
  private BigDecimal total;
  private String pdfUrl;
  private OffsetDateTime issuedAt;
  private OffsetDateTime receivedAt;
  private OffsetDateTime createdAt;

  @PrePersist public void pre(){
    if(id==null) id=UUID.randomUUID();
    if(createdAt==null) createdAt=OffsetDateTime.now();
  }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public String getDocType() { return docType; }
  public void setDocType(String docType) { this.docType = docType; }
  public String getDocNumber() { return docNumber; }
  public void setDocNumber(String docNumber) { this.docNumber = docNumber; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getNet() { return net; }
  public void setNet(BigDecimal net) { this.net = net; }
  public BigDecimal getVat() { return vat; }
  public void setVat(BigDecimal vat) { this.vat = vat; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public String getPdfUrl() { return pdfUrl; }
  public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
  public OffsetDateTime getIssuedAt() { return issuedAt; }
  public void setIssuedAt(OffsetDateTime issuedAt) { this.issuedAt = issuedAt; }
  public OffsetDateTime getReceivedAt() { return receivedAt; }
  public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
