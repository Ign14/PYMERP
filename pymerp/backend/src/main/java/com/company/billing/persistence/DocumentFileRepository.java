package com.company.billing.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {

  List<DocumentFile> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);

  List<DocumentFile> findByDocumentIdAndVersion(UUID documentId, DocumentFileVersion version);
}
