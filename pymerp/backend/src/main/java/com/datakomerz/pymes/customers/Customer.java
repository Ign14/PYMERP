package com.datakomerz.pymes.customers;

import com.datakomerz.pymes.audit.AuditableEntity;
import com.datakomerz.pymes.multitenancy.TenantFiltered;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET active=false WHERE id=?")
@SQLRestriction("active=true")
@TenantFiltered
public class Customer extends AuditableEntity {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 20)
  private String rut;

  @Column(columnDefinition = "text")
  private String address;

  @Column(precision = 10, scale = 6)
  private BigDecimal lat;

  @Column(precision = 10, scale = 6)
  private BigDecimal lng;

  @Column(length = 20)
  private String phone;

  @Column(length = 120)
  private String email;

  @Column(length = 64)
  private String segment;

  @Column(name = "contact_person", length = 120)
  private String contactPerson;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(nullable = false)
  private Boolean active = true;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (active == null) {
      active = true;
    }
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

}
