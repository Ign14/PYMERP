package com.datakomerz.pymes.billing.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for keeping track of billing idempotency keys in Redis.
 */
@ConfigurationProperties(prefix = "billing.idempotency")
public class BillingIdempotencyProperties {

  /**
   * TTL that applies to idempotency records.
   */
  private Duration ttl = Duration.ofHours(6);

  /**
   * Maximum time to wait for a concurrent request to finish.
   */
  private Duration waitTimeout = Duration.ofSeconds(5);

  /**
   * Polling interval when waiting for a sibling request to complete.
   */
  private Duration waitPollInterval = Duration.ofMillis(250);

  public Duration getTtl() {
    return ttl;
  }

  public void setTtl(Duration ttl) {
    this.ttl = ttl;
  }

  public Duration getWaitTimeout() {
    return waitTimeout;
  }

  public void setWaitTimeout(Duration waitTimeout) {
    this.waitTimeout = waitTimeout;
  }

  public Duration getWaitPollInterval() {
    return waitPollInterval;
  }

  public void setWaitPollInterval(Duration waitPollInterval) {
    this.waitPollInterval = waitPollInterval;
  }
}
