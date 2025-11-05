package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspecto que habilita automáticamente el filtro de tenant antes de ejecutar
 * operaciones en repositorios JPA.
 * 
 * <p>Este aspecto intercepta llamadas a repositorios Spring Data JPA y habilita
 * el filtro de Hibernate antes de ejecutar cualquier query.</p>
 * 
 * <p><strong>Ventajas:</strong></p>
 * <ul>
 *   <li>Filtrado automático - No requiere código manual en cada repository</li>
 *   <li>Seguridad - Imposible olvidar aplicar el filtro</li>
 *   <li>Transparente - Los repositories no necesitan cambios</li>
 * </ul>
 * 
 * <p><strong>Limitaciones:</strong></p>
 * <ul>
 *   <li>Solo funciona con entidades marcadas con @TenantFiltered</li>
 *   <li>Requiere que TenantContext esté establecido (por TenantInterceptor)</li>
 * </ul>
 * 
 * @see TenantFilterEnabler
 * @see TenantContext
 * @see TenantFiltered
 * @since Sprint 5
 */
@Aspect
@Component
public class TenantFilterAspect {

  private static final Logger logger = LoggerFactory.getLogger(TenantFilterAspect.class);

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TenantFilterEnabler filterEnabler;

  /**
   * Habilita el filtro de tenant antes de ejecutar cualquier método en repositorios JPA.
   * 
   * <p>Este advice se ejecuta antes de cualquier método en clases que implementan
   * JpaRepository, CrudRepository, etc.</p>
   */
  @Before("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
  public void enableTenantFilter() {
    if (TenantContext.isPresent()) {
      filterEnabler.enableTenantFilter(entityManager);
    } else {
      logger.trace("No tenant context present, skipping filter");
    }
  }
}
