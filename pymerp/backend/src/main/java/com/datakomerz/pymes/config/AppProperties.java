package com.datakomerz.pymes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Cors cors = new Cors();
  private final Tenancy tenancy = new Tenancy();
  private final Security security = new Security();

  public Cors getCors() {
    return cors;
  }

  public Tenancy getTenancy() {
    return tenancy;
  }

  public Security getSecurity() {
    return security;
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

  public static class Security {
    private final Jwt jwt = new Jwt();
    private final Captcha captcha = new Captcha();

    public Jwt getJwt() {
      return jwt;
    }

    public Captcha getCaptcha() {
      return captcha;
    }

    public static class Jwt {
      private String secret;
      private long expirationSeconds = 3600; // 1h access token default
      private long refreshExpirationSeconds = 2592000; // 30 days
      private boolean oidcEnabled = false;

      public boolean isOidcEnabled() {
        return oidcEnabled;
      }

      public void setOidcEnabled(boolean oidcEnabled) {
        this.oidcEnabled = oidcEnabled;
      }

      public String getSecret() {
        return secret;
      }

      public void setSecret(String secret) {
        this.secret = secret;
      }

      public long getExpirationSeconds() {
        return expirationSeconds;
      }

      public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
      }

      public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
      }

      public void setRefreshExpirationSeconds(long refreshExpirationSeconds) {
        this.refreshExpirationSeconds = refreshExpirationSeconds;
      }
    }

    public static class Captcha {
      private boolean enabled = true;
      private int minOperand = 0;
      private int maxOperand = 50;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public int getMinOperand() {
        return minOperand;
      }

      public void setMinOperand(int minOperand) {
        this.minOperand = minOperand;
      }

      public int getMaxOperand() {
        return maxOperand;
      }

      public void setMaxOperand(int maxOperand) {
        this.maxOperand = maxOperand;
      }
    }
  }
}
