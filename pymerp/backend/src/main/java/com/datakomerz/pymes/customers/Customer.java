package com.datakomerz.pymes.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
  private UUID companyId;

  @Column(nullable = false, length = 120)
  private String name;

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

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public void setCompanyId(UUID companyId) {
    this.companyId = companyId;
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
}
