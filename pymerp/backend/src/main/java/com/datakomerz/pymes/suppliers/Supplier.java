package com.datakomerz.pymes.suppliers;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity @Table(name="suppliers")
public class Supplier {
  @Id @Column(columnDefinition="uuid")
  private UUID id;
  @Column(name="company_id", nullable=false, columnDefinition="uuid")
  private UUID companyId;
  @NotBlank
  @Column(nullable=false, length=100)
  private String name;
  @Column(length=20)
  private String rut;
  @Column(name="created_at")
  private OffsetDateTime createdAt;

  @PrePersist public void prePersist() {
    if(id==null) id=UUID.randomUUID();
    if(createdAt==null) createdAt=OffsetDateTime.now();
  }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getCompanyId() { return companyId; }
  public void setCompanyId(UUID companyId) { this.companyId = companyId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getRut() { return rut; }
  public void setRut(String rut) { this.rut = rut; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
