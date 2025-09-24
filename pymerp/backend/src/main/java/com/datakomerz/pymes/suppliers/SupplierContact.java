package com.datakomerz.pymes.suppliers;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity @Table(name="supplier_contacts")
public class SupplierContact {
  @Id @Column(columnDefinition="uuid")
  private UUID id;
  @Column(name="supplier_id", nullable=false, columnDefinition="uuid")
  private UUID supplierId;
  @NotBlank
  private String name;
  private String title;
  private String phone;
  private String email;

  @PrePersist public void prePersist(){ if(id==null) id=UUID.randomUUID(); }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
}
