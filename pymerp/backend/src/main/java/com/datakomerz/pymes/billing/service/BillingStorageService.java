package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import java.io.IOException;
import java.util.UUID;

public interface BillingStorageService {

  StoredFile store(UUID documentId,
                   DocumentFileKind kind,
                   DocumentFileVersion version,
                   byte[] content,
                   String filename,
                   String contentType) throws IOException;

  record StoredFile(String storageKey, String checksum) {
  }
}
