package com.datakomerz.pymes.audit;

import java.lang.annotation.*;

/**
 * Anotación para marcar endpoints que deben generar audit logs.
 * Se aplica a nivel de método en controllers.
 *
 * Ejemplo:
 * <pre>
 * {@code
 * @Audited(action = "DELETE", entityType = "Customer")
 * @DeleteMapping("/{id}")
 * public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

  /**
   * Acción ejecutada: CREATE, READ, UPDATE, DELETE, etc.
   * Por defecto se infiere del método HTTP.
   */
  String action() default "";

  /**
   * Tipo de entidad afectada: Customer, Product, Sale, etc.
   * Este valor aparecerá en audit_logs.entity_type
   */
  String entityType();

  /**
   * Si es true, captura el request body en el audit log.
   * Por defecto es false para evitar overhead.
   * NOTA: Nunca capturar passwords o tokens sensibles.
   */
  boolean captureRequestBody() default false;
}
