package com.datakomerz.pymes.security.jwt;

import com.datakomerz.pymes.config.SecurityProperties;
import com.datakomerz.pymes.security.AppUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecurityProperties securityProperties;

  public JwtService(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public String generateToken(AppUserDetails user) {
    Instant now = Instant.now();
    long expiry = securityProperties.getJwt().getExpirationSeconds();
    Instant expiration = now.plusSeconds(expiry);
    Set<String> roles = user.getAuthorities().stream()
      .map(Object::toString)
      .collect(Collectors.toSet());

    return Jwts.builder()
      .id(UUID.randomUUID().toString())
      .subject(user.getUsername())
      .issuedAt(Date.from(now))
      .expiration(Date.from(expiration))
      .claims(Map.of(
        "companyId", user.getCompanyId().toString(),
        "roles", roles
      ))
      .signWith(signingKey())
      .compact();
  }

  public boolean isTokenValid(String token, String username) {
    Claims claims = parseClaims(token);
    String subject = claims.getSubject();
    Instant expiration = claims.getExpiration().toInstant();
    return subject.equalsIgnoreCase(username) && expiration.isAfter(Instant.now());
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
      .verifyWith(signingKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  private SecretKey signingKey() {
    String secret = securityProperties.getJwt().getSecret();
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 characters");
    }
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
