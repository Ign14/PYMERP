package com.datakomerz.pymes.audit;

import com.datakomerz.pymes.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST API para consultar audit logs.
 * Solo accesible por usuarios con rol ADMIN.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

  private final AuditService auditService;

  public AuditLogController(AuditService auditService) {
    this.auditService = auditService;
  }

  /**
   * GET /api/v1/audit/logs
   * Obtiene todos los audit logs de la empresa del usuario autenticado.
   *
   * @param page Número de página (0-indexed)
   * @param size Tamaño de página
   * @return Página de audit logs
   */
  @GetMapping("/logs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AuditLog>> getAuditLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Long companyId = SecurityUtils.getCurrentUserCompanyId()
        .orElseThrow(() -> new IllegalStateException("Company ID not found in token"));

    Pageable pageable = PageRequest.of(page, size);
    Page<AuditLog> logs = auditService.getAuditLogs(companyId, pageable);
    return ResponseEntity.ok(logs);
  }

  /**
   * GET /api/v1/audit/logs/user/{username}
   * Obtiene audit logs de un usuario específico.
   *
   * @param username Username del usuario
   * @param page Número de página
   * @param size Tamaño de página
   * @return Página de audit logs
   */
  @GetMapping("/logs/user/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
      @PathVariable String username,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Long companyId = SecurityUtils.getCurrentUserCompanyId()
        .orElseThrow(() -> new IllegalStateException("Company ID not found in token"));

    Pageable pageable = PageRequest.of(page, size);
    Page<AuditLog> logs = auditService.getAuditLogsByUser(username, companyId, pageable);
    return ResponseEntity.ok(logs);
  }

  /**
   * GET /api/v1/audit/logs/action/{action}
   * Obtiene audit logs filtrados por acción (ej: ACCESS_DENIED, DELETE).
   *
   * @param action Acción a filtrar (CREATE, READ, UPDATE, DELETE, LOGIN, ACCESS_DENIED, etc.)
   * @param page Número de página
   * @param size Tamaño de página
   * @return Página de audit logs
   */
  @GetMapping("/logs/action/{action}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
      @PathVariable String action,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Long companyId = SecurityUtils.getCurrentUserCompanyId()
        .orElseThrow(() -> new IllegalStateException("Company ID not found in token"));

    Pageable pageable = PageRequest.of(page, size);
    Page<AuditLog> logs = auditService.getAuditLogsByAction(action, companyId, pageable);
    return ResponseEntity.ok(logs);
  }

  /**
   * GET /api/v1/audit/logs/failed
   * Obtiene audit logs de requests fallidos (status code >= 400).
   *
   * @param page Número de página
   * @param size Tamaño de página
   * @return Página de audit logs con errores
   */
  @GetMapping("/logs/failed")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AuditLog>> getFailedRequests(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Long companyId = SecurityUtils.getCurrentUserCompanyId()
        .orElseThrow(() -> new IllegalStateException("Company ID not found in token"));

    Pageable pageable = PageRequest.of(page, size);
    Page<AuditLog> logs = auditService.getFailedRequests(companyId, pageable);
    return ResponseEntity.ok(logs);
  }

  /**
   * GET /api/v1/audit/logs/range
   * Obtiene audit logs en un rango de fechas.
   *
   * @param startDate Fecha de inicio (ISO 8601, ej: 2025-01-01T00:00:00Z)
   * @param endDate Fecha de fin (ISO 8601)
   * @param page Número de página
   * @param size Tamaño de página
   * @return Página de audit logs
   */
  @GetMapping("/logs/range")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AuditLog>> getAuditLogsBetween(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Long companyId = SecurityUtils.getCurrentUserCompanyId()
        .orElseThrow(() -> new IllegalStateException("Company ID not found in token"));

    Pageable pageable = PageRequest.of(page, size);
    Page<AuditLog> logs = auditService.getAuditLogsBetween(startDate, endDate, companyId, pageable);
    return ResponseEntity.ok(logs);
  }

  /**
   * GET /api/v1/audit/security/failed-attempts/{username}
   * Obtiene el número de intentos fallidos de acceso en las últimas 24 horas.
   * Útil para detectar ataques de fuerza bruta.
   *
   * @param username Username a verificar
   * @return Número de intentos fallidos
   */
  @GetMapping("/security/failed-attempts/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Long> getFailedAccessAttempts(@PathVariable String username) {
    long count = auditService.countFailedAccessAttempts(username, 24); // últimas 24 horas
    return ResponseEntity.ok(count);
  }
}
