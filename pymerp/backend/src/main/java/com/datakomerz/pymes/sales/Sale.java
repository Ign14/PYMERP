package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales", indexes = {
  @Index(name = "idx_sales_company", columnList = "company_id")
})
@TenantFiltered
@Schema(name = "Sale", description = "Venta registrada en el ERP")
public class Sale extends TenantAwareEntity {
  @Id
  @Column(columnDefinition="uuid")
  @Schema(description = "ID de la venta", example = "4f1b9d85-9021-4798-a5f4-0b5a53da9b2a")
  private UUID id;

  @Column(name="customer_id", columnDefinition="uuid")
  @Schema(description = "ID del cliente asociado", example = "c98a3d2b-8dd3-4a61-9d81-6f561dd9c7c5")
  private UUID customerId;

  @Column(nullable=false)
  @Schema(description = "Estado de la venta", example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
  private String status;

  @Column(nullable=false, precision=14, scale=2)
  @Schema(description = "Monto neto sin impuestos", example = "1200000.00")
  private BigDecimal net;

  @Column(nullable=false, precision=14, scale=2)
  @Schema(description = "Impuesto IVA asociado", example = "228000.00")
  private BigDecimal vat;

  @Column(nullable=false, precision=14, scale=2)
  @Schema(description = "Monto total de la venta", example = "1428000.00")
  private BigDecimal total;

  @Schema(description = "Método de pago utilizado", example = "TRANSFERENCIA")
  private String paymentMethod;

  @Column(name="issued_at")
  @Schema(description = "Fecha/hora de emisión", example = "2024-03-05T15:20:00Z")
  private OffsetDateTime issuedAt;

  @Schema(description = "Tipo de documento tributario", example = "FACTURA")
  private String docType;

  @Schema(description = "URL del PDF o respaldo", example = "https://cdn.pymerp.cl/sales/4f1b9d85.pdf")
  private String pdfUrl;

  @PrePersist public void pre(){ if(id==null) id=UUID.randomUUID(); if(issuedAt==null) issuedAt=OffsetDateTime.now(); }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
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
