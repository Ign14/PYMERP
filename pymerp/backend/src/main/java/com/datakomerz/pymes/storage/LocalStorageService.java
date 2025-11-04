package com.datakomerz.pymes.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class LocalStorageService implements StorageService {
  private final StorageProperties properties;
  private final Path basePath;
  private final String publicPrefix;

  public LocalStorageService(StorageProperties properties) throws IOException {
    this.properties = properties;
    this.basePath = properties.basePath();
    this.publicPrefix = properties.getPublicUrl();
    Files.createDirectories(basePath);
  }

  @Override
  public String storeProductImage(UUID companyId, UUID productId, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Image file is empty");
    }
    String extension = resolveImageExtension(file.getContentType(), file.getOriginalFilename());
    String filename = "image-" + System.currentTimeMillis() + extension;
    Path directory = resolveProductDirectory(companyId, productId);
    Files.createDirectories(directory);
    Path target = directory.resolve(filename);
    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return buildPublicUrl(companyId, productId, filename);
  }

  @Override
  public String storeProductQr(UUID companyId, UUID productId, byte[] content, String extension) throws IOException {
    String safeExtension = extension != null && !extension.isBlank() ? extension : "png";
    if (!safeExtension.startsWith(".")) {
      safeExtension = "." + safeExtension;
    }
    String filename = "qr" + safeExtension.toLowerCase(Locale.ROOT);
    Path directory = resolveProductDirectory(companyId, productId);
    Files.createDirectories(directory);
    Path target = directory.resolve(filename);
    Files.write(target, content);
    return buildPublicUrl(companyId, productId, filename);
  }

  @Override
  public String storePurchaseDocument(UUID companyId, UUID purchaseId, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Document file is empty");
    }
    String extension = extractExtension(file.getOriginalFilename());
    String filename = "document-" + System.currentTimeMillis() + extension;
    Path directory = resolvePurchaseDirectory(companyId, purchaseId);
    Files.createDirectories(directory);
    Path target = directory.resolve(filename);
    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return buildPurchasePublicUrl(companyId, purchaseId, filename);
  }

  @Override
  public StoredFile load(String publicUrl) throws IOException {
    if (publicUrl == null || publicUrl.isBlank()) {
      throw new IllegalArgumentException("File path cannot be empty");
    }
    if (!publicUrl.startsWith(publicPrefix)) {
      throw new IllegalArgumentException("File is outside configured storage prefix");
    }
    String relative = publicUrl.substring(publicPrefix.length());
    if (relative.startsWith("/")) {
      relative = relative.substring(1);
    }
    Path file = basePath.resolve(relative).normalize();
    if (!file.startsWith(basePath)) {
      throw new IllegalArgumentException("Attempt to access file outside storage root");
    }
    if (!Files.exists(file)) {
      throw new IOException("File not found: " + publicUrl);
    }
    Resource resource;
    try {
      resource = new UrlResource(file.toUri());
    } catch (MalformedURLException e) {
      throw new IOException("Invalid file path", e);
    }
    String contentType = Files.probeContentType(file);
    if (contentType == null && file.toString().toLowerCase(Locale.ROOT).endsWith(".svg")) {
      contentType = "image/svg+xml";
    }
    String filename = file.getFileName().toString();
    return new StoredFile(resource, filename, contentType);
  }

  private Path resolveProductDirectory(UUID companyId, UUID productId) {
    return basePath
      .resolve("tenants")
      .resolve(companyId.toString())
      .resolve("products")
      .resolve(productId.toString());
  }

  private Path resolvePurchaseDirectory(UUID companyId, UUID purchaseId) {
    return basePath
      .resolve("tenants")
      .resolve(companyId.toString())
      .resolve("purchases")
      .resolve(purchaseId.toString());
  }

  private String buildPublicUrl(UUID companyId, UUID productId, String filename) {
    String suffix = String.join("/",
      "tenants",
      companyId.toString(),
      "products",
      productId.toString(),
      filename
    );
    if (publicPrefix.endsWith("/")) {
      return publicPrefix + suffix;
    }
    return publicPrefix + "/" + suffix;
  }

  private String buildPurchasePublicUrl(UUID companyId, UUID purchaseId, String filename) {
    String suffix = String.join("/",
      "tenants",
      companyId.toString(),
      "purchases",
      purchaseId.toString(),
      filename
    );
    if (publicPrefix.endsWith("/")) {
      return publicPrefix + suffix;
    }
    return publicPrefix + "/" + suffix;
  }

  private String resolveImageExtension(String contentType, String originalFilename) {
    if (contentType != null) {
      return switch (contentType) {
        case MediaType.IMAGE_PNG_VALUE -> ".png";
        case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
        case "image/webp" -> ".webp";
        default -> extractExtension(originalFilename);
      };
    }
    return extractExtension(originalFilename);
  }

  private String extractExtension(String name) {
    if (name == null) {
      return ".bin";
    }
    String cleaned = StringUtils.cleanPath(name);
    int dot = cleaned.lastIndexOf('.');
    if (dot >= 0) {
      return cleaned.substring(dot).toLowerCase(Locale.ROOT);
    }
    return ".bin";
  }
}
