package com.datakomerz.pymes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

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
    private long expirationSeconds = 3600;
    private long refreshExpirationSeconds = 2592000;
    private boolean oidcEnabled = false;

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

    public boolean isOidcEnabled() {
      return oidcEnabled;
    }

    public void setOidcEnabled(boolean oidcEnabled) {
      this.oidcEnabled = oidcEnabled;
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
