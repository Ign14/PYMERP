package com.datakomerz.pymes.audit;

import com.datakomerz.pymes.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Interceptor que captura requests HTTP y genera audit logs.
 * Se ejecuta antes y después de cada request para medir tiempo de respuesta.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(AuditInterceptor.class);

  private final AuditService auditService;

  private static final String REQUEST_START_TIME = "auditStartTime";
  private static final String AUDIT_ANNOTATION = "auditAnnotation";

  public AuditInterceptor(AuditService auditService) {
    this.auditService = auditService;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) throws Exception {

    // Solo procesar @Audited en HandlerMethods
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    Method method = handlerMethod.getMethod();
    Audited auditedAnnotation = method.getAnnotation(Audited.class);

    if (auditedAnnotation == null) {
      return true; // No tiene @Audited, continuar sin auditar
    }

    // Guardar timestamp de inicio
    request.setAttribute(REQUEST_START_TIME, Instant.now());
    request.setAttribute(AUDIT_ANNOTATION, auditedAnnotation);

    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex) throws Exception {

    if (!(handler instanceof HandlerMethod)) {
      return;
    }

    Audited auditedAnnotation = (Audited) request.getAttribute(AUDIT_ANNOTATION);
    if (auditedAnnotation == null) {
      return; // No se marcó en preHandle
    }

    Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
    Long responseTimeMs = null;
    if (startTime != null) {
      responseTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
    }

    // Construir AuditLog
    AuditLog log = new AuditLog();
    log.setTimestamp(Instant.now());

    // Usuario autenticado
    String username = SecurityUtils.getCurrentUsername().orElse("anonymous");
    log.setUsername(username);

    // Roles
    String roles = SecurityUtils.getCurrentUserRoles().stream()
        .collect(Collectors.joining(","));
    log.setUserRoles(roles);

    // Acción: usar annotation o inferir de HTTP method
    String action = auditedAnnotation.action().isEmpty()
        ? inferActionFromHttpMethod(request.getMethod())
        : auditedAnnotation.action();
    log.setAction(action);

    // Entity type
    log.setEntityType(auditedAnnotation.entityType());

    // Entity ID: extraer de path variable si existe /{id}
    Long entityId = extractEntityIdFromPath(request.getRequestURI());
    log.setEntityId(entityId);

    // HTTP details
    log.setHttpMethod(request.getMethod());
    log.setEndpoint(request.getRequestURI());
    log.setIpAddress(getClientIp(request));
    log.setUserAgent(request.getHeader("User-Agent"));

    // Company ID: obtener del contexto de seguridad
    Long companyId = SecurityUtils.getCurrentUserCompanyId().orElse(null);
    log.setCompanyId(companyId);

    // Status code
    log.setStatusCode(response.getStatus());

    // Error message si hubo excepción
    if (ex != null) {
      log.setErrorMessage(ex.getMessage());
    }

    // Request body (solo si annotation lo permite)
    if (auditedAnnotation.captureRequestBody()) {
      String requestBody = extractRequestBody(request);
      log.setRequestBody(requestBody);
    }

    // Response time
    log.setResponseTimeMs(responseTimeMs);

    // Guardar log de forma asíncrona
    try {
      auditService.logAction(log);
    } catch (Exception e) {
      logger.error("Error al guardar audit log: {}", e.getMessage(), e);
      // No lanzar excepción para no afectar el flujo principal
    }
  }

  /**
   * Infiere la acción del método HTTP:
   * POST -> CREATE, GET -> READ, PUT/PATCH -> UPDATE, DELETE -> DELETE
   */
  private String inferActionFromHttpMethod(String httpMethod) {
    return switch (httpMethod) {
      case "POST" -> "CREATE";
      case "GET" -> "READ";
      case "PUT", "PATCH" -> "UPDATE";
      case "DELETE" -> "DELETE";
      default -> httpMethod;
    };
  }

  /**
   * Extrae el ID de la entidad del path (ej: /api/v1/customers/123 -> 123)
   */
  private Long extractEntityIdFromPath(String uri) {
    try {
      String[] parts = uri.split("/");
      String lastPart = parts[parts.length - 1];
      return Long.parseLong(lastPart);
    } catch (NumberFormatException e) {
      return null; // No hay ID numérico en el path
    }
  }

  /**
   * Obtiene la IP real del cliente, considerando proxies (X-Forwarded-For)
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  /**
   * Extrae el request body del request.
   * NOTA: Limitado a 4000 caracteres para evitar overhead.
   */
  private String extractRequestBody(HttpServletRequest request) {
    try (BufferedReader reader = request.getReader()) {
      String body = reader.lines().collect(Collectors.joining("\n"));
      if (body.length() > 4000) {
        return body.substring(0, 4000) + "... [truncated]";
      }
      return body;
    } catch (IOException e) {
      logger.warn("No se pudo leer request body: {}", e.getMessage());
      return null;
    }
  }
}
