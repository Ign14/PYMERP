package com.datakomerz.pymes.multitenancy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se detecta un intento de acceso cross-tenant.
 * 
 * <p>Esta excepción protege contra bugs de seguridad donde un usuario
 * intenta acceder a datos de otra empresa (tenant).</p>
 * 
 * <p>Casos donde se lanza:</p>
 * <ul>
 *   <li>Usuario de empresa A intenta modificar entidad de empresa B</li>
 *   <li>Query manual sin filtro de company_id</li>
 *   <li>Validación explícita con @ValidateTenant detecta inconsistencia</li>
 * </ul>
 * 
 * <p>La anotación @ResponseStatus hace que Spring convierta esta excepción
 * en un HTTP 403 Forbidden automáticamente.</p>
 * 
 * <p><strong>Ejemplo:</strong></p>
 * <pre>
 * // Usuario de empresa A (UUID: 123) intenta acceder a producto de empresa B (UUID: 456)
 * Product product = productRepository.findById(productId);
 * if (!product.getCompanyId().equals(TenantContext.getTenantId())) {
 *   throw new CrossTenantAccessException("Cannot access product from different tenant");
 * }
 * </pre>
 * 
 * @see TenantContext
 * @see TenantValidationAspect
 * @since Sprint 5
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Cross-tenant access denied")
public class CrossTenantAccessException extends RuntimeException {

  /**
   * Crea una nueva excepción con el mensaje especificado.
   * 
   * @param message mensaje descriptivo del error
   */
  public CrossTenantAccessException(String message) {
    super(message);
  }

  /**
   * Crea una nueva excepción con mensaje y causa.
   * 
   * @param message mensaje descriptivo del error
   * @param cause causa raíz de la excepción
   */
  public CrossTenantAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
