package com.datakomerz.pymes.customers;

import com.datakomerz.pymes.multitenancy.TenantAwareEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
@TenantFiltered
@Schema(name = "Customer", description = "Cliente registrado en el ERP")
public class Customer extends TenantAwareEntity {

  @Id
  @Column(columnDefinition = "uuid")
  @Schema(description = "ID del cliente", example = "c98a3d2b-8dd3-4a61-9d81-6f561dd9c7c5")
  private UUID id;

  @Column(nullable = false, length = 120)
  @Schema(description = "Nombre o razón social", example = "Comercial Acme Ltda.", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Column(length = 20)
  @Schema(description = "RUT o identificador tributario", example = "76.543.210-9")
  private String rut;

  @Column(columnDefinition = "text")
  @Schema(description = "Dirección principal", example = "Av. Providencia 1234, Santiago")
  private String address;

  @Column(precision = 10, scale = 6)
  @Schema(description = "Latitud geográfica", example = "-33.420123")
  private BigDecimal lat;

  @Column(precision = 10, scale = 6)
  @Schema(description = "Longitud geográfica", example = "-70.611234")
  private BigDecimal lng;

  @Column(length = 20)
  @Schema(description = "Teléfono de contacto", example = "+56 9 9876 5432")
  private String phone;

  @Column(length = 120)
  @Schema(description = "Email de contacto", example = "contacto@acme.cl")
  private String email;

  @Column(length = 64)
  @Schema(description = "Segmento o categoría comercial", example = "Retail")
  private String segment;

  @Column(name = "contact_person", length = 120)
  @Schema(description = "Persona de contacto principal", example = "María Pérez")
  private String contactPerson;

  @Column(columnDefinition = "text")
  @Schema(description = "Notas internas o comentarios", example = "Requiere factura electrónica")
  private String notes;

  @Column(nullable = false)
  @Schema(description = "Indicador de cliente activo", example = "true")
  private Boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Schema(description = "Fecha de creación", example = "2024-02-01T10:00:00Z")
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @Schema(description = "Última actualización", example = "2024-02-10T09:30:00Z")
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
    if (active == null) {
      active = true;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public BigDecimal getLat() {
    return lat;
  }

  public void setLat(BigDecimal lat) {
    this.lat = lat;
  }

  public BigDecimal getLng() {
    return lng;
  }

  public void setLng(BigDecimal lng) {
    this.lng = lng;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSegment() {
    return segment;
  }

  public void setSegment(String segment) {
    this.segment = segment;
  }

  public String getRut() {
    return rut;
  }

  public void setRut(String rut) {
    this.rut = rut;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
