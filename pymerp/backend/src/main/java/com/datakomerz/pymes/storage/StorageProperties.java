package com.datakomerz.pymes.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
  private String basePath = "storage";
  private String publicUrl = "/storage";

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    if (basePath != null && !basePath.isBlank()) {
      this.basePath = basePath;
    }
  }

  public String getPublicUrl() {
    return publicUrl.startsWith("/") ? publicUrl : "/" + publicUrl;
  }

  public void setPublicUrl(String publicUrl) {
    if (publicUrl != null && !publicUrl.isBlank()) {
      this.publicUrl = publicUrl;
    }
  }

  public Path basePath() {
    return Paths.get(basePath).toAbsolutePath().normalize();
  }
}
