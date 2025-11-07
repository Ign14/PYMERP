package com.datakomerz.pymes.purchases;

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
@Table(name = "purchases", indexes = {
  @Index(name = "idx_purchases_company", columnList = "company_id")
})
@TenantFiltered
@Schema(name = "Purchase", description = "Compra registrada a proveedores")
public class Purchase extends TenantAwareEntity {
  @Id
  @Column(columnDefinition="uuid")
  @Schema(description = "ID de la compra", example = "a9f6f3ea-38bb-4fa7-9c8e-1f65b8cb85f1")
  private UUID id;

  @Column(name="supplier_id", columnDefinition="uuid")
  @Schema(description = "ID del proveedor asociado", example = "86f6d6ad-8afb-4b73-9dbf-9b78f779a28d")
  private UUID supplierId;

  @Schema(description = "Tipo de documento", example = "FACTURA")
  private String docType;

  @Schema(description = "Número de documento", example = "F123-987654")
  private String docNumber;

  @Schema(description = "Estado de la compra", example = "RECEIVED")
  private String status;

  @Schema(description = "Monto neto", example = "950000.00")
  private BigDecimal net;

  @Schema(description = "Monto de impuestos", example = "180500.00")
  private BigDecimal vat;

  @Schema(description = "Monto total", example = "1130500.00")
  private BigDecimal total;

  @Schema(description = "URL del PDF o respaldo", example = "https://cdn.pymerp.cl/purchases/a9f6f3ea.pdf")
  private String pdfUrl;

  @Schema(description = "Fecha de emisión", example = "2024-02-28T12:00:00Z")
  private OffsetDateTime issuedAt;

  @Schema(description = "Fecha de recepción en bodega", example = "2024-03-02T09:15:00Z")
  private OffsetDateTime receivedAt;

  @Schema(description = "Fecha de creación del registro", example = "2024-02-28T12:00:00Z")
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
