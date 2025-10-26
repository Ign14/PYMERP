package com.datakomerz.pymes.billing.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "billing.offline")
public class BillingOfflineProperties {

  private boolean enabled = true;

  @Valid
  @NotNull
  private Retry retry = new Retry();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Retry getRetry() {
    return retry;
  }

  public void setRetry(Retry retry) {
    this.retry = retry;
  }

  public static class Retry {

    @Min(1)
    private int maxAttempts = 5;

    @Positive
    private long backoffMs = 30000;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getBackoffMs() {
      return backoffMs;
    }

    public void setBackoffMs(long backoffMs) {
      this.backoffMs = backoffMs;
    }
  }
}
