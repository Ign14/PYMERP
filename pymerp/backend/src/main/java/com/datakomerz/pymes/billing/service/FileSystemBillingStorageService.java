package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

@Component
public class FileSystemBillingStorageService implements BillingStorageService {

  private final Path root;

  public FileSystemBillingStorageService(
      @Value("${billing.storage.base-path:storage/billing}") String basePath) throws IOException {
    this.root = Paths.get(basePath).toAbsolutePath().normalize();
    Files.createDirectories(root);
  }

  @Override
  public StoredFile store(UUID documentId,
                          DocumentFileKind kind,
                          DocumentFileVersion version,
                          byte[] content,
                          String filename,
                          String contentType) throws IOException {
    if (documentId == null) {
      throw new IllegalArgumentException("documentId is required");
    }
    if (kind == null) {
      throw new IllegalArgumentException("kind is required");
    }
    if (version == null) {
      throw new IllegalArgumentException("version is required");
    }
    if (content == null || content.length == 0) {
      throw new IllegalArgumentException("content is empty");
    }
    String sanitizedFilename = sanitizeFilename(filename, version);
    Path targetDirectory = root
        .resolve(kind.name().toLowerCase(Locale.ROOT))
        .resolve(documentId.toString());
    Files.createDirectories(targetDirectory);
    Path targetFile = targetDirectory.resolve(sanitizedFilename);
    Files.write(targetFile, content);
    String checksum = computeChecksum(content);
    String storageKey = root.relativize(targetFile).toString().replace('\\', '/');
    return new StoredFile(storageKey, checksum);
  }

  @Override
  public byte[] read(String storageKey) throws IOException {
    Resource resource = loadAsResource(storageKey);
    try (var input = resource.getInputStream()) {
      return input.readAllBytes();
    }
  }

  @Override
  public Resource loadAsResource(String storageKey) throws IOException {
    if (storageKey == null || storageKey.isBlank()) {
      throw new IllegalArgumentException("storageKey is required");
    }
    if (storageKey.startsWith("http://") || storageKey.startsWith("https://")) {
      try {
        return new UrlResource(URI.create(storageKey));
      } catch (IllegalArgumentException ex) {
        throw new IOException("Invalid external storageKey: " + storageKey, ex);
      }
    }
    Path path = resolveLocalPath(storageKey);
    try {
      Resource resource = new UrlResource(path.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      }
      throw new IOException("Stored file not readable: " + storageKey);
    } catch (MalformedURLException ex) {
      throw new IOException("Invalid path for storageKey: " + storageKey, ex);
    }
  }

  private Path resolveLocalPath(String storageKey) throws IOException {
    Path path = root.resolve(storageKey).normalize();
    if (!path.startsWith(root)) {
      throw new IOException("storageKey outside of storage root");
    }
    if (!Files.exists(path)) {
      throw new IOException("Stored file not found: " + storageKey);
    }
    return path;
  }

  private String sanitizeFilename(String filename, DocumentFileVersion version) {
    String fallback = version.name().toLowerCase(Locale.ROOT) + ".pdf";
    if (filename == null || filename.isBlank()) {
      return fallback;
    }
    String sanitized = filename.replaceAll("[^A-Za-z0-9._-]", "_");
    if (sanitized.isBlank()) {
      return fallback;
    }
    return sanitized;
  }

  private String computeChecksum(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 digest not available", ex);
    }
  }
}
