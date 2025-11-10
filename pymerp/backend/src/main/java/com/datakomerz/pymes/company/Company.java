package com.datakomerz.pymes.company;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "business_name", nullable = false, length = 160)
  private String businessName;

  @Column(name = "fantasy_name", length = 160)
  private String fantasyName;

  @Column(nullable = false, length = 20, unique = true)
  private String rut;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "business_activity", length = 120)
  private String businessActivity;

  @Column(length = 160)
  private String address;

  @Column(length = 80)
  private String commune;

  @Column(length = 40)
  private String phone;

  @Column(length = 160)
  private String email;

  @Column(name = "receipt_footer_message", columnDefinition = "text")
  private String receiptFooterMessage;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getBusinessName() {
    return businessName;
  }

  public void setBusinessName(String businessName) {
    this.businessName = businessName;
  }

  public String getFantasyName() {
    return fantasyName;
  }

  public void setFantasyName(String fantasyName) {
    this.fantasyName = fantasyName;
  }

  public String getRut() {
    return rut;
  }

  public void setRut(String rut) {
    this.rut = rut;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getBusinessActivity() {
    return businessActivity;
  }

  public void setBusinessActivity(String businessActivity) {
    this.businessActivity = businessActivity;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCommune() {
    return commune;
  }

  public void setCommune(String commune) {
    this.commune = commune;
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

  public String getReceiptFooterMessage() {
    return receiptFooterMessage;
  }

  public void setReceiptFooterMessage(String receiptFooterMessage) {
    this.receiptFooterMessage = receiptFooterMessage;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
