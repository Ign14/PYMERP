package com.datakomerz.pymes.auth;

import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.auth.dto.RefreshRequest;
import com.datakomerz.pymes.config.AppProperties;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
  private static final String REFRESH_HEADER = "X-Refresh-Token";
  private static final String REFRESH_COOKIE = "refresh_token";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final List<String> ALL_MODULES = List.of(
    "dashboard",
    "sales",
    "purchases",
    "inventory",
    "customers",
    "suppliers",
    "finances",
    "reports",
    "settings"
  );

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final AppProperties appProperties;
  private final RefreshTokenService refreshTokenService;
  private final CompanyContext companyContext;

  public AuthController(AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        AppProperties appProperties,
                        RefreshTokenService refreshTokenService,
                        CompanyContext companyContext) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.appProperties = appProperties;
    this.refreshTokenService = refreshTokenService;
    this.companyContext = companyContext;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(request.email(), request.password())
    );

    AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
    String token = jwtService.generateToken(principal);
    long expiresIn = appProperties.getSecurity().getJwt().getExpirationSeconds();
    String refreshToken = refreshTokenService.issueToken(principal.getAccount());
    long refreshExpiresIn = appProperties.getSecurity().getJwt().getRefreshExpirationSeconds();

    AuthResponse response = buildResponse(principal, token, expiresIn, refreshToken, refreshExpiresIn);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Operation(summary = "Refresh authentication tokens",
      description = "Usa el refresh token enviado en la cabecera X-Refresh-Token, en Authorization Bearer o cookie refresh_token para emitir nuevos tokens",
      parameters = {
        @Parameter(name = REFRESH_HEADER, in = ParameterIn.HEADER, description = "Refresh token crudo", required = false),
        @Parameter(name = HttpHeaders.AUTHORIZATION, in = ParameterIn.HEADER, description = "Authorization Bearer con el refresh token", required = false),
        @Parameter(name = REFRESH_COOKIE, in = ParameterIn.COOKIE, description = "Cookie con el refresh token", required = false)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tokens renovados"),
    @ApiResponse(responseCode = "401", description = "Refresh token invalido o expirado"),
    @ApiResponse(responseCode = "403", description = "Usuario deshabilitado o tenant invalido")
  })
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest servletRequest,
                                              @RequestBody(required = false) RefreshRequest requestBody) {
    Span span = Span.current();
    span.addEvent("auth.refresh.begin");

    String rawRefreshToken = resolveRefreshToken(servletRequest, requestBody);

    var rotationResult = refreshTokenService.rotateToken(rawRefreshToken);
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

    AuthResponse response = buildResponse(userDetails, token, expiresIn, rotationResult.refreshToken(), refreshExpiresIn);
    span.addEvent("auth.refresh.success");
    span.setStatus(StatusCode.OK);
    LOG.info("Refresh token rotated for user {}", userDetails.getUsername());
    return ResponseEntity.ok(response);
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

  private AuthResponse buildResponse(AppUserDetails principal,
                                     String token,
                                     long expiresIn,
                                     String refreshToken,
                                     long refreshExpiresIn) {
    Set<String> roles = principal.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.toSet());
    Set<String> modules = resolveModules(roles);

    return new AuthResponse(
      token,
      expiresIn,
      refreshToken,
      refreshExpiresIn,
      principal.getCompanyId(),
      principal.getUsername(),
      principal.getDisplayName(),
      roles,
      modules
    );
  }

  private Set<String> resolveModules(Collection<String> roles) {
    Set<String> modules = new LinkedHashSet<>();
    if (roles == null || roles.isEmpty()) {
      modules.add("dashboard");
      return modules;
    }
    for (String role : roles) {
      if (role == null) {
        continue;
      }
      String normalized = role.trim().toUpperCase(Locale.ROOT);
      switch (normalized) {
        case "ROLE_ADMIN" -> modules.addAll(ALL_MODULES);
        case "ROLE_SALES", "ROLE_SELLER" -> modules.addAll(List.of("dashboard", "sales", "customers", "reports"));
        case "ROLE_PURCHASES", "ROLE_BUYER" -> modules.addAll(List.of("dashboard", "purchases", "suppliers", "inventory", "reports"));
        case "ROLE_INVENTORY" -> modules.addAll(List.of("dashboard", "inventory", "suppliers", "reports"));
        case "ROLE_FINANCE" -> modules.addAll(List.of("dashboard", "finances", "reports"));
        case "ROLE_REPORTS" -> modules.addAll(List.of("dashboard", "reports"));
        case "ROLE_SETTINGS" -> modules.add("settings");
        default -> {
        }
      }
    }
    if (modules.isEmpty()) {
      modules.add("dashboard");
    }
    modules.add("dashboard");
    return modules;
  }
}
