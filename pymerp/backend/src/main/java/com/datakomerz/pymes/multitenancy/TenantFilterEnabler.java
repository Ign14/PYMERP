package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Componente que habilita el filtro de tenant en las sesiones de Hibernate.
 * 
 * <p>Este componente proporciona métodos para activar dinámicamente el filtro de tenant
 * basado en el contexto actual. El filtro se aplica a todas las entidades marcadas
 * con @TenantFiltered.</p>
 * 
 * <p><strong>IMPORTANTE:</strong> El filtro debe habilitarse en cada sesión/transacción
 * porque Hibernate no mantiene los filtros entre sesiones.</p>
 * 
 * <p><strong>Cómo funciona:</strong></p>
 * <ol>
 *   <li>EntityManager se unwrap a Hibernate Session</li>
 *   <li>Se habilita el filtro "tenantFilter"</li>
 *   <li>Se establece el parámetro tenantId desde TenantContext</li>
 *   <li>Todas las queries a entidades @TenantFiltered incluyen automáticamente WHERE company_id = :tenantId</li>
 * </ol>
 * 
 * @see TenantContext
 * @see TenantFiltered
 * @since Sprint 5
 */
@Component
public class TenantFilterEnabler {

  private static final Logger logger = LoggerFactory.getLogger(TenantFilterEnabler.class);
  
  /**
   * Nombre del filtro de Hibernate definido en las entidades.
   */
  public static final String TENANT_FILTER_NAME = "tenantFilter";
  
  /**
   * Nombre del parámetro del filtro.
   */
  public static final String TENANT_PARAMETER_NAME = "tenantId";

  /**
   * Habilita el filtro de tenant en la sesión actual del EntityManager.
   * 
   * <p>Este método debe llamarse al inicio de cada transacción/request que requiera
   * filtrado por tenant.</p>
   * 
   * @param entityManager EntityManager de JPA
   * @return true si el filtro se habilitó correctamente, false si no hay tenant context
   */
  public boolean enableTenantFilter(EntityManager entityManager) {
    UUID tenantId = TenantContext.getTenantId();
    
    if (tenantId == null) {
      logger.debug("No tenant context available, filter not enabled");
      return false;
    }
    
    try {
      Session session = entityManager.unwrap(Session.class);
      session.enableFilter(TENANT_FILTER_NAME)
             .setParameter(TENANT_PARAMETER_NAME, tenantId);
      
      logger.debug("Tenant filter enabled for tenant: {}", tenantId);
      return true;
    } catch (Exception e) {
      logger.warn("Failed to enable tenant filter: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Deshabilita el filtro de tenant en la sesión actual.
   * 
   * @param entityManager EntityManager de JPA
   */
  public void disableTenantFilter(EntityManager entityManager) {
    try {
      Session session = entityManager.unwrap(Session.class);
      session.disableFilter(TENANT_FILTER_NAME);
      logger.debug("Tenant filter disabled");
    } catch (Exception e) {
      logger.warn("Failed to disable tenant filter: {}", e.getMessage());
    }
  }
}
