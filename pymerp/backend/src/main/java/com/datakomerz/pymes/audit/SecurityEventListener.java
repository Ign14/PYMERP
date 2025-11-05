package com.datakomerz.pymes.audit;

import com.datakomerz.pymes.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Listener para eventos de seguridad de Spring Security.
 * Captura LOGIN (success/failure) y ACCESS_DENIED (403).
 */
@Component
public class SecurityEventListener {

  private static final Logger logger = LoggerFactory.getLogger(SecurityEventListener.class);

  private final AuditService auditService;

  public SecurityEventListener(AuditService auditService) {
    this.auditService = auditService;
  }

  /**
   * Captura eventos de autenticación exitosa (LOGIN).
   */
  @EventListener
  public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    String username = event.getAuthentication().getName();
    logger.info("Login exitoso: {}", username);

    AuditLog log = new AuditLog();
    log.setTimestamp(Instant.now());
    log.setUsername(username);
    log.setAction("LOGIN");
    log.setEntityType("Authentication");

    // Roles
    String roles = event.getAuthentication().getAuthorities().stream()
        .map(Object::toString)
        .collect(Collectors.joining(","));
    log.setUserRoles(roles);

    // HTTP details desde request actual
    HttpServletRequest request = getCurrentHttpRequest();
    if (request != null) {
      log.setEndpoint(request.getRequestURI());
      log.setHttpMethod(request.getMethod());
      log.setIpAddress(getClientIp(request));
      log.setUserAgent(request.getHeader("User-Agent"));
    }

    log.setStatusCode(200); // Login exitoso

    auditService.logAction(log);
  }

  /**
   * Captura eventos de autenticación fallida (FAILED_LOGIN).
   */
  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    String username = event.getAuthentication().getName();
    logger.warn("Login fallido para usuario: {}", username);

    AuditLog log = new AuditLog();
    log.setTimestamp(Instant.now());
    log.setUsername(username != null ? username : "unknown");
    log.setAction("FAILED_LOGIN");
    log.setEntityType("Authentication");

    // HTTP details
    HttpServletRequest request = getCurrentHttpRequest();
    if (request != null) {
      log.setEndpoint(request.getRequestURI());
      log.setHttpMethod(request.getMethod());
      log.setIpAddress(getClientIp(request));
      log.setUserAgent(request.getHeader("User-Agent"));
    }

    // Error
    log.setStatusCode(401); // Unauthorized
    log.setErrorMessage(event.getException().getMessage());

    auditService.logAction(log);
  }

  /**
   * Captura eventos de autorización denegada (403 Forbidden).
   */
  @EventListener
  public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
    String username = SecurityUtils.getCurrentUsername().orElse("anonymous");
    logger.warn("Acceso denegado para usuario: {}", username);

    AuditLog log = new AuditLog();
    log.setTimestamp(Instant.now());
    log.setUsername(username);
    log.setAction("ACCESS_DENIED");

    // Roles
    String roles = SecurityUtils.getCurrentUserRoles().stream()
        .collect(Collectors.joining(","));
    log.setUserRoles(roles);

    // HTTP details
    HttpServletRequest request = getCurrentHttpRequest();
    if (request != null) {
      log.setEndpoint(request.getRequestURI());
      log.setHttpMethod(request.getMethod());
      log.setIpAddress(getClientIp(request));
      log.setUserAgent(request.getHeader("User-Agent"));
      log.setEntityType(extractEntityTypeFromPath(request.getRequestURI()));
    }

    // Company ID
    Long companyId = SecurityUtils.getCurrentUserCompanyId().orElse(null);
    log.setCompanyId(companyId);

    // Error
    log.setStatusCode(403); // Forbidden
    log.setErrorMessage("Insufficient permissions: " + event.getAuthorizationDecision());

    auditService.logAction(log);
  }

  /**
   * Obtiene el HttpServletRequest del contexto actual.
   */
  private HttpServletRequest getCurrentHttpRequest() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      return attributes != null ? attributes.getRequest() : null;
    } catch (Exception e) {
      return null; // No hay request en el contexto (ej: eventos no-HTTP)
    }
  }

  /**
   * Obtiene la IP real del cliente, considerando proxies (X-Forwarded-For).
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  /**
   * Extrae el tipo de entidad del path (ej: /api/v1/customers/123 -> "Customer").
   */
  private String extractEntityTypeFromPath(String uri) {
    if (uri == null || !uri.startsWith("/api/")) {
      return "Unknown";
    }

    String[] parts = uri.split("/");
    if (parts.length >= 4) {
      String entityPath = parts[3]; // ej: "customers" en /api/v1/customers/123
      // Capitalizar primera letra: customers -> Customer
      return entityPath.substring(0, 1).toUpperCase() + entityPath.substring(1);
    }

    return "Unknown";
  }
}
