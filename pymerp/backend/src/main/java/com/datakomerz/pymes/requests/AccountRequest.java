package com.datakomerz.pymes.requests;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_account_requests")
public class AccountRequest {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false, length = 20)
  private String rut;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(nullable = false, length = 200)
  private String address;

  @Column(nullable = false, length = 150)
  private String email;

  @Column(name = "company_name", nullable = false, length = 150)
  private String companyName;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AccountRequestStatus status;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "processed_at")
  private OffsetDateTime processedAt;

  @Column(name = "processed_by", columnDefinition = "uuid")
  private UUID processedBy;

  @Column(name = "processed_by_username", length = 120)
  private String processedByUsername;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "text")
  private String userAgent;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (status == null) {
      status = AccountRequestStatus.PENDING;
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getRut() {
    return rut;
  }

  public void setRut(String rut) {
    this.rut = rut;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public AccountRequestStatus getStatus() {
    return status;
  }

  public void setStatus(AccountRequestStatus status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(OffsetDateTime processedAt) {
    this.processedAt = processedAt;
  }

  public UUID getProcessedBy() {
    return processedBy;
  }

  public void setProcessedBy(UUID processedBy) {
    this.processedBy = processedBy;
  }

  public String getProcessedByUsername() {
    return processedByUsername;
  }

  public void setProcessedByUsername(String processedByUsername) {
    this.processedByUsername = processedByUsername;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
