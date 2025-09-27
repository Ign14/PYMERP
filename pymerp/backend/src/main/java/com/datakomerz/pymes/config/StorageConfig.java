package com.datakomerz.pymes.config;

import com.datakomerz.pymes.storage.LocalStorageService;
import com.datakomerz.pymes.storage.StorageProperties;
import com.datakomerz.pymes.storage.StorageService;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig implements WebMvcConfigurer {
  private final StorageProperties properties;

  public StorageConfig(StorageProperties properties) {
    this.properties = properties;
  }

  @Bean
  public StorageService storageService() throws IOException {
    return new LocalStorageService(properties);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String publicPrefix = properties.getPublicUrl();
    Path basePath = properties.basePath();
    String location = basePath.toUri().toString();
    registry.addResourceHandler(publicPrefix + "/**")
      .addResourceLocations(location)
      .setCachePeriod(3600);
  }
}
