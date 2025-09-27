package com.datakomerz.pymes.storage;

import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
  String storeProductImage(UUID companyId, UUID productId, MultipartFile file) throws IOException;

  String storeProductQr(UUID companyId, UUID productId, byte[] content, String extension) throws IOException;

  StoredFile load(String publicUrl) throws IOException;

  record StoredFile(Resource resource, String filename, String contentType) {}
}
