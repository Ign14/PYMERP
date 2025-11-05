package com.datakomerz.pymes.multitenancy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se requiere un tenant context pero no está disponible.
 * 
 * <p>Casos comunes:</p>
 * <ul>
 *   <li>Request sin header X-Company-Id</li>
 *   <li>Header X-Company-Id con formato inválido</li>
 *   <li>Código intenta usar TenantContext.require() sin contexto establecido</li>
 * </ul>
 * 
 * <p>La anotación @ResponseStatus hace que Spring convierta esta excepción
 * en un HTTP 400 Bad Request automáticamente.</p>
 * 
 * @see TenantContext
 * @see TenantInterceptor
 * @since Sprint 5
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Tenant context not found")
public class TenantNotFoundException extends RuntimeException {

  /**
   * Crea una nueva excepción con el mensaje especificado.
   * 
   * @param message mensaje descriptivo del error
   */
  public TenantNotFoundException(String message) {
    super(message);
  }

  /**
   * Crea una nueva excepción con mensaje y causa.
   * 
   * @param message mensaje descriptivo del error
   * @param cause causa raíz de la excepción
   */
  public TenantNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
