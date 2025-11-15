package com.datakomerz.pymes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Cors cors = new Cors();
  private final Tenancy tenancy = new Tenancy();

  public Cors getCors() {
    return cors;
  }

  public Tenancy getTenancy() {
    return tenancy;
  }

  public static class Cors {
    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedOriginPatterns = new ArrayList<>();

    public List<String> getAllowedOrigins() {
      return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOriginPatterns() {
      return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
      this.allowedOriginPatterns = allowedOriginPatterns;
    }
  }

  public static class Tenancy {
    private UUID defaultCompanyId;

    public UUID getDefaultCompanyId() {
      return defaultCompanyId;
    }

    public void setDefaultCompanyId(UUID defaultCompanyId) {
      this.defaultCompanyId = defaultCompanyId;
    }
  }

}
