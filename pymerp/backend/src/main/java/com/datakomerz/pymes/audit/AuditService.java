package com.datakomerz.pymes.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servicio para gestionar audit logs.
 * Operaciones asíncronas para no bloquear el flujo principal.
 */
@Service
public class AuditService {

  private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /**
   * Guarda un audit log de forma asíncrona.
   * No bloquea el hilo principal.
   */
  @Async
  @Transactional
  public void logAction(AuditLog auditLog) {
    try {
      auditLogRepository.save(auditLog);
      logger.debug("Audit log guardado: {}", auditLog);
    } catch (Exception e) {
      logger.error("Error al guardar audit log: {}", e.getMessage(), e);
    }
  }

  /**
   * Obtiene logs de auditoría para una empresa, con paginación.
   */
  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditLogs(Long companyId, Pageable pageable) {
    return auditLogRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable);
  }

  /**
   * Obtiene logs de un usuario específico en una empresa.
   */
  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditLogsByUser(String username, Long companyId, Pageable pageable) {
    return auditLogRepository.findByUsernameAndCompanyIdOrderByTimestampDesc(
        username, companyId, pageable);
  }

  /**
   * Obtiene logs por acción (ej: "ACCESS_DENIED", "DELETE").
   */
  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditLogsByAction(String action, Long companyId, Pageable pageable) {
    return auditLogRepository.findByActionAndCompanyIdOrderByTimestampDesc(
        action, companyId, pageable);
  }

  /**
   * Obtiene logs en un rango de fechas.
   */
  @Transactional(readOnly = true)
  public Page<AuditLog> getAuditLogsBetween(
      Instant startDate, Instant endDate, Long companyId, Pageable pageable) {
    return auditLogRepository.findByTimestampBetweenAndCompanyId(
        startDate, endDate, companyId, pageable);
  }

  /**
   * Obtiene logs con errores (status code >= 400).
   */
  @Transactional(readOnly = true)
  public Page<AuditLog> getFailedRequests(Long companyId, Pageable pageable) {
    return auditLogRepository.findByStatusCodeGreaterThanEqualAndCompanyId(
        400, companyId, pageable);
  }

  /**
   * Obtiene intentos fallidos de acceso (403 Forbidden) en las últimas N horas.
   * Útil para detectar ataques de fuerza bruta.
   */
  @Transactional(readOnly = true)
  public long countFailedAccessAttempts(String username, int hoursAgo) {
    Instant since = Instant.now().minus(hoursAgo, ChronoUnit.HOURS);
    return auditLogRepository.countFailedAccessAttempts(username, since);
  }
}
