package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity 
@Table(name="suppliers")
@TenantFiltered
@Schema(name = "Supplier", description = "Proveedor registrado para realizar compras")
public class Supplier extends TenantAwareEntity {
  @Id @Column(columnDefinition="uuid")
  @Schema(description = "ID del proveedor", example = "86f6d6ad-8afb-4b73-9dbf-9b78f779a28d")
  private UUID id;
  @NotBlank
  @Column(nullable=false, length=100)
  @Schema(description = "Nombre legal o fantasia", example = "Servicios Logísticos Andes", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;
  @Column(length=20)
  @Schema(description = "RUT o identificación tributaria", example = "77.123.456-1")
  private String rut;
  @Column(length=200)
  @Schema(description = "Dirección comercial", example = "Av. Las Industrias 3456, Santiago")
  private String address;
  @Column(length=120)
  @Schema(description = "Comuna o ciudad", example = "Maipú")
  private String commune;
  @Column(name="business_activity", length=120)
  @Schema(description = "Giro o actividad principal", example = "Transporte y logística")
  private String businessActivity;
  @Column(length=30)
  @Schema(description = "Teléfono de contacto", example = "+56 2 2987 6543")
  private String phone;
  @Column(length=120)
  @Schema(description = "Correo electrónico", example = "contacto@logisticosandes.cl")
  private String email;
  @Column(nullable=false)
  @Schema(description = "Indicador de proveedor activo", example = "true")
  private Boolean active;
  @Column(name="created_at")
  @Schema(description = "Fecha de creación del registro", example = "2024-01-05T08:00:00Z")
  private OffsetDateTime createdAt;

  @PrePersist public void prePersist() {
    if(id==null) id=UUID.randomUUID();
    if(createdAt==null) createdAt=OffsetDateTime.now();
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
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
