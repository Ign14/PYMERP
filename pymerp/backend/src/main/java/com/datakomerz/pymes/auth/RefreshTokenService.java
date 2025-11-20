package com.datakomerz.pymes.auth;

import com.datakomerz.pymes.config.SecurityProperties;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenService.class);
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String TOKEN_INVALID_CODE = "TOKEN_INVALID";

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityProperties securityProperties;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                             UserAccountRepository userAccountRepository,
                             PasswordEncoder passwordEncoder,
                             SecurityProperties securityProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.securityProperties = securityProperties;
  }

  @Transactional
  public String issueToken(UserAccount user) {
    revokeAll(user.getId());
    return format(createToken(user));
  }

  @Transactional
  public RotationResult rotateToken(String compoundToken) {
    Span span = Span.current();
    span.addEvent("refresh_token.rotate.start");

    if (compoundToken == null || compoundToken.isBlank()) {
      throw invalid("Refresh token missing", span);
    }

    TokenParts parts = parse(compoundToken, span);

    RefreshToken existing = refreshTokenRepository.findById(parts.id())
      .orElseThrow(() -> invalid("Refresh token not found", span));

    OffsetDateTime now = OffsetDateTime.now();
    if (existing.getRevokedAt() != null || existing.getExpiresAt().isBefore(now)) {
      LOG.warn("Refresh token {} rejected: expired or revoked", existing.getId());
      span.addEvent("refresh_token.expired");
      span.setStatus(StatusCode.ERROR, "Expired or revoked refresh token");
      throw invalid("Refresh token expired or revoked", span);
    }

    if (!passwordEncoder.matches(parts.raw(), existing.getTokenHash())) {
      LOG.warn("Refresh token {} rejected: hash mismatch", existing.getId());
      span.addEvent("refresh_token.mismatch");
      span.setStatus(StatusCode.ERROR, "Refresh token mismatch");
      throw invalid("Refresh token mismatch", span);
    }

    UserAccount user = userAccountRepository.findById(existing.getUserId())
      .orElseThrow(() -> invalid("User not found for refresh token", span));

    existing.setRevokedAt(now);
    refreshTokenRepository.save(existing);

    if (!"active".equalsIgnoreCase(user.getStatus())) {
      LOG.warn("Refresh rejected for disabled user {}", user.getEmail());
      span.addEvent("refresh_token.user_disabled");
      span.setStatus(StatusCode.ERROR, "User disabled");
      throw new UserDisabledException("Refresh token rejected for disabled user");
    }

    RawRefreshToken newToken;
    do {
      newToken = createToken(user);
    } while (format(newToken).equals(compoundToken));

    span.addEvent("refresh_token.rotate.success");
    span.setStatus(StatusCode.OK);
    return new RotationResult(user, format(newToken));
  }

  @Transactional
  public void revokeAll(UUID userId) {
    List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
    OffsetDateTime now = OffsetDateTime.now();
    for (RefreshToken token : tokens) {
      token.setRevokedAt(now);
    }
    refreshTokenRepository.saveAll(tokens);
  }

  private RawRefreshToken createToken(UserAccount user) {
    String rawToken = generateRawToken();
    RefreshToken refresh = new RefreshToken();
    refresh.setUserId(user.getId());
    refresh.setTokenHash(passwordEncoder.encode(rawToken));
    refresh.setExpiresAt(OffsetDateTime.now().plusSeconds(securityProperties.getJwt().getRefreshExpirationSeconds()));
    RefreshToken persisted = refreshTokenRepository.save(refresh);
    return new RawRefreshToken(persisted.getId(), rawToken);
  }

  private String format(RawRefreshToken token) {
    return token.id() + ":" + token.raw();
  }

  private TokenParts parse(String compoundToken, Span span) {
    String[] parts = compoundToken.split(":", 2);
    if (parts.length != 2) {
      throw invalid("Invalid refresh token format", span);
    }
    try {
      return new TokenParts(UUID.fromString(parts[0]), parts[1]);
    } catch (IllegalArgumentException ex) {
      throw invalid("Invalid refresh token format", span);
    }
  }

  private InvalidRefreshTokenException invalid(String message, Span span) {
    if (span != null) {
      span.addEvent("refresh_token.invalid");
      span.setStatus(StatusCode.ERROR, message);
    }
    return new InvalidRefreshTokenException(TOKEN_INVALID_CODE, message);
  }

  private String generateRawToken() {
    byte[] bytes = new byte[48];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record RotationResult(UserAccount user, String refreshToken) {}

  private record RawRefreshToken(UUID id, String raw) {}

  private record TokenParts(UUID id, String raw) {}
}
