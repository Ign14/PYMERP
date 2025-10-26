package com.datakomerz.pymes.billing.security;

import com.datakomerz.pymes.billing.config.BillingWebhookProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BillingWebhookSignatureVerifier {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final HexFormat HEX = HexFormat.of();

  private final BillingWebhookProperties properties;
  private final Clock clock;

  public BillingWebhookSignatureVerifier(BillingWebhookProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public void verify(String signatureHeader, String requestBody) {
    if (signatureHeader == null || signatureHeader.isBlank()) {
      throw unauthorized("Missing X-Signature header");
    }
    String secret = properties.getSecret();
    if (secret == null || secret.isBlank()) {
      throw unauthorized("Webhook secret not configured");
    }
    Map<String, String> parts = parseSignature(signatureHeader);
    String timestampValue = parts.get("t");
    String signatureValue = parts.get("v1");
    if (timestampValue == null || signatureValue == null) {
      throw unauthorized("Invalid signature format");
    }

    long timestampSeconds;
    try {
      timestampSeconds = Long.parseLong(timestampValue);
    } catch (NumberFormatException ex) {
      throw unauthorized("Invalid signature timestamp");
    }

    Instant timestamp = Instant.ofEpochSecond(timestampSeconds);
    Duration tolerance = Optional.ofNullable(properties.getTolerance())
        .orElse(Duration.ofMinutes(5));
    Duration skew = Duration.between(timestamp, clock.instant()).abs();
    if (skew.compareTo(tolerance) > 0) {
      throw unauthorized("Signature timestamp outside tolerance");
    }

    String payloadToSign = timestampValue + "." + (requestBody != null ? requestBody : "");
    String expectedSignature = computeSignature(secret, payloadToSign);
    if (!constantTimeEquals(expectedSignature, signatureValue)) {
      throw unauthorized("Signature mismatch");
    }
  }

  private Map<String, String> parseSignature(String signatureHeader) {
    Map<String, String> result = new HashMap<>();
    String[] entries = signatureHeader.split(",");
    for (String entry : entries) {
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String[] keyValue = trimmed.split("=", 2);
      if (keyValue.length == 2) {
        result.put(keyValue[0], keyValue[1]);
      }
    }
    return result;
  }

  private String computeSignature(String secret, String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
      mac.init(key);
      byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(digest);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Unable to compute webhook signature", ex);
    }
  }

  private boolean constantTimeEquals(String expectedHex, String provided) {
    // FIX: normalize both signatures to lowercase ASCII bytes before comparison.
    String normalizedExpected = normalizeSignature(expectedHex);
    String normalizedProvided = normalizeSignature(provided);
    byte[] expectedBytes = normalizedExpected.getBytes(StandardCharsets.US_ASCII);
    byte[] providedBytes = normalizedProvided.getBytes(StandardCharsets.US_ASCII);
    return java.security.MessageDigest.isEqual(expectedBytes, providedBytes);
  }

  private String normalizeSignature(String signature) {
    String value = signature == null ? "" : signature.trim();
    if (value.regionMatches(true, 0, "v1=", 0, 3)) {
      value = value.substring(3);
    }
    value = value.replaceAll("\\s+", "");
    return value.toLowerCase(Locale.ROOT);
  }

  private ResponseStatusException unauthorized(String reason) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
  }
}
