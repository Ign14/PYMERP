package com.datakomerz.pymes.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio para consultar logs de auditoría.
 * Todas las consultas deben filtrar por companyId para multi-tenancy.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /**
   * Obtener todos los logs de una empresa ordenados por timestamp descendente.
   */
  Page<AuditLog> findByCompanyIdOrderByTimestampDesc(Long companyId, Pageable pageable);

  /**
   * Obtener logs de un usuario específico en una empresa.
   */
  Page<AuditLog> findByUsernameAndCompanyIdOrderByTimestampDesc(
    String username, Long companyId, Pageable pageable
  );

  /**
   * Obtener logs por acción (ej: "ACCESS_DENIED", "DELETE") en una empresa.
   */
  Page<AuditLog> findByActionAndCompanyIdOrderByTimestampDesc(
    String action, Long companyId, Pageable pageable
  );

  /**
   * Obtener logs por tipo de entidad (ej: "Customer", "Product") en una empresa.
   */
  Page<AuditLog> findByEntityTypeAndCompanyIdOrderByTimestampDesc(
    String entityType, Long companyId, Pageable pageable
  );

  /**
   * Obtener logs en un rango de fechas para una empresa.
   */
  @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate " +
         "AND a.companyId = :companyId ORDER BY a.timestamp DESC")
  Page<AuditLog> findByTimestampBetweenAndCompanyId(
    @Param("startDate") Instant startDate,
    @Param("endDate") Instant endDate,
    @Param("companyId") Long companyId,
    Pageable pageable
  );

  /**
   * Obtener logs con errores (status code 4xx, 5xx) en una empresa.
   */
  @Query("SELECT a FROM AuditLog a WHERE a.statusCode >= :minStatusCode " +
         "AND a.companyId = :companyId ORDER BY a.timestamp DESC")
  Page<AuditLog> findByStatusCodeGreaterThanEqualAndCompanyId(
    @Param("minStatusCode") Integer minStatusCode,
    @Param("companyId") Long companyId,
    Pageable pageable
  );

  /**
   * Contar intentos fallidos de acceso para un usuario en las últimas N horas.
   * Útil para detectar ataques de fuerza bruta.
   */
  @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.username = :username " +
         "AND a.action = 'ACCESS_DENIED' " +
         "AND a.timestamp > :since")
  long countFailedAccessAttempts(
    @Param("username") String username,
    @Param("since") Instant since
  );

  /**
   * Obtener actividad de un usuario en una empresa (últimas 100 acciones).
   * Útil para investigaciones de seguridad.
   */
  @Query("SELECT a FROM AuditLog a WHERE a.username = :username " +
         "AND a.companyId = :companyId ORDER BY a.timestamp DESC")
  List<AuditLog> findRecentActivityByUser(
    @Param("username") String username,
    @Param("companyId") Long companyId,
    Pageable pageable
  );
}
