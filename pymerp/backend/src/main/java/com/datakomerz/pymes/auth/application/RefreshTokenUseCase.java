package com.datakomerz.pymes.auth.application;

import com.datakomerz.pymes.auth.InvalidRefreshTokenException;
import com.datakomerz.pymes.auth.RefreshTokenService;
import com.datakomerz.pymes.auth.RefreshTokenService.RotationResult;
import com.datakomerz.pymes.auth.TenantMismatchException;
import com.datakomerz.pymes.auth.UserDisabledException;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.auth.dto.RefreshRequest;
import com.datakomerz.pymes.config.AppProperties;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RefreshTokenUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenUseCase.class);
  private static final String REFRESH_HEADER = "X-Refresh-Token";
  private static final String REFRESH_COOKIE = "refresh_token";
  private static final String BEARER_PREFIX = "Bearer ";

  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final AppProperties appProperties;
  private final CompanyContext companyContext;
  private final AuthResponseFactory authResponseFactory;

  public RefreshTokenUseCase(RefreshTokenService refreshTokenService,
                             JwtService jwtService,
                             AppProperties appProperties,
                             CompanyContext companyContext,
                             AuthResponseFactory authResponseFactory) {
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.appProperties = appProperties;
    this.companyContext = companyContext;
    this.authResponseFactory = authResponseFactory;
  }

  public AuthResponse handle(HttpServletRequest request, RefreshRequest requestBody) {
    Span span = Span.current();
    span.addEvent("auth.refresh.begin");

    String rawRefreshToken = resolveRefreshToken(request, requestBody);

    RotationResult rotationResult = refreshTokenService.rotateToken(rawRefreshToken);
    AppUserDetails userDetails = new AppUserDetails(rotationResult.user());

    if (!userDetails.isEnabled()) {
      span.addEvent("auth.refresh.user_disabled");
      span.setStatus(StatusCode.ERROR, "User disabled");
      LOG.warn("Refresh attempt for disabled user {}", userDetails.getUsername());
      throw new UserDisabledException("User is disabled");
    }

    validateTenant(userDetails, span);

    String token = jwtService.generateToken(userDetails);
    long expiresIn = appProperties.getSecurity().getJwt().getExpirationSeconds();
    long refreshExpiresIn = appProperties.getSecurity().getJwt().getRefreshExpirationSeconds();

    AuthResponse response = authResponseFactory.build(
      userDetails,
      token,
      expiresIn,
      rotationResult.refreshToken(),
      refreshExpiresIn
    );

    span.addEvent("auth.refresh.success");
    span.setStatus(StatusCode.OK);
    LOG.info("Refresh token rotated for user {}", userDetails.getUsername());
    return response;
  }

  private void validateTenant(AppUserDetails userDetails, Span span) {
    Optional<UUID> maybeCompany = companyContext.current();
    if (maybeCompany.isEmpty()) {
      return;
    }
    UUID requestCompany = maybeCompany.get();
    UUID userCompany = userDetails.getCompanyId();
    if (!userCompany.equals(requestCompany)) {
      span.addEvent("auth.refresh.tenant_mismatch");
      span.setStatus(StatusCode.ERROR, "Tenant mismatch");
      LOG.warn("Tenant mismatch during refresh: request {} vs user {}", requestCompany, userCompany);
      throw new TenantMismatchException("Tenant mismatch for refresh token");
    }
  }

  private String resolveRefreshToken(HttpServletRequest request, RefreshRequest body) {
    String headerToken = request.getHeader(REFRESH_HEADER);
    if (StringUtils.hasText(headerToken)) {
      return headerToken.trim();
    }

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
      return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (REFRESH_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
          return cookie.getValue().trim();
        }
      }
    }

    if (body != null && StringUtils.hasText(body.refreshToken())) {
      return body.refreshToken().trim();
    }

    throw new InvalidRefreshTokenException("TOKEN_INVALID", "Refresh token missing");
  }
}
