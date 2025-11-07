package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.audit.AuditableEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity 
@Table(name="suppliers")
@TenantFiltered
public class Supplier extends AuditableEntity {
  @Id @Column(columnDefinition="uuid")
  private UUID id;
  @NotBlank
  @Column(nullable=false, length=100)
  private String name;
  @Column(length=20)
  private String rut;
  @Column(length=200)
  private String address;
  @Column(length=120)
  private String commune;
  @Column(name="business_activity", length=120)
  private String businessActivity;
  @Column(length=30)
  private String phone;
  @Column(length=120)
  private String email;
  @Column(nullable=false)
  private Boolean active;
  @PrePersist public void prePersist() {
    if(id==null) id=UUID.randomUUID();
    if(active==null) active=true;
  }

  // Getters & Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getRut() { return rut; }
  public void setRut(String rut) { this.rut = rut; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getCommune() { return commune; }
  public void setCommune(String commune) { this.commune = commune; }
  public String getBusinessActivity() { return businessActivity; }
  public void setBusinessActivity(String businessActivity) { this.businessActivity = businessActivity; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
}
