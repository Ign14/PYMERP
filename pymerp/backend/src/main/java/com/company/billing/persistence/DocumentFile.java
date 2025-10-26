package com.company.billing.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "document_files")
public class DocumentFile {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @NotNull
  @Column(name = "document_id", nullable = false, columnDefinition = "uuid")
  private UUID documentId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 16)
  private DocumentFileKind kind;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "version", nullable = false, length = 16)
  private DocumentFileVersion version;

  @NotNull
  @Size(max = 120)
  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @NotNull
  @Size(max = 255)
  @Column(name = "storage_key", nullable = false, length = 255)
  private String storageKey;

  @Size(max = 120)
  @Column(name = "checksum", length = 120)
  private String checksum;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_file_id")
  private DocumentFile previousFile;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void prePersist() {
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

  public UUID getDocumentId() {
    return documentId;
  }

  public void setDocumentId(UUID documentId) {
    this.documentId = documentId;
  }

  public DocumentFileKind getKind() {
    return kind;
  }

  public void setKind(DocumentFileKind kind) {
    this.kind = kind;
  }

  public DocumentFileVersion getVersion() {
    return version;
  }

  public void setVersion(DocumentFileVersion version) {
    this.version = version;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public DocumentFile getPreviousFile() {
    return previousFile;
  }

  public void setPreviousFile(DocumentFile previousFile) {
    this.previousFile = previousFile;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
