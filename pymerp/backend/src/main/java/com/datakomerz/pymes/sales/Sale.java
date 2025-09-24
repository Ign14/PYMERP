package com.datakomerz.pymes.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="sales")
public class Sale {
  @Id @Column(columnDefinition="uuid") private UUID id;
  @Column(name="company_id", nullable=false, columnDefinition="uuid") private UUID companyId;
  @Column(name="customer_id", columnDefinition="uuid") private UUID customerId;
  @Column(nullable=false) private String status;
  @Column(nullable=false, precision=14, scale=2) private BigDecimal net;
  @Column(nullable=false, precision=14, scale=2) private BigDecimal vat;
  @Column(nullable=false, precision=14, scale=2) private BigDecimal total;
  private String paymentMethod;
  @Column(name="issued_at") private OffsetDateTime issuedAt;
  private String docType;
  private String pdfUrl;

  @PrePersist public void pre(){ if(id==null) id=UUID.randomUUID(); if(issuedAt==null) issuedAt=OffsetDateTime.now(); }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getCompanyId() { return companyId; }
  public void setCompanyId(UUID companyId) { this.companyId = companyId; }
  public UUID getCustomerId() { return customerId; }
  public void setCustomerId(UUID customerId) { this.customerId = customerId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getNet() { return net; }
  public void setNet(BigDecimal net) { this.net = net; }
  public BigDecimal getVat() { return vat; }
  public void setVat(BigDecimal vat) { this.vat = vat; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public OffsetDateTime getIssuedAt() { return issuedAt; }
  public void setIssuedAt(OffsetDateTime issuedAt) { this.issuedAt = issuedAt; }
  public String getDocType() { return docType; }
  public void setDocType(String docType) { this.docType = docType; }
  public String getPdfUrl() { return pdfUrl; }
  public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}