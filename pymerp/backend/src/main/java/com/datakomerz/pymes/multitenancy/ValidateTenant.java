package com.datakomerz.pymes.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or service method that must ensure the targeted entity
 * belongs to the tenant present in {@link TenantContext}.
 *
 * <p>The annotated method should expose the entity identifier either by using
 * the default parameter name {@code id} or by specifying a different name via
 * {@link #entityParam()}. When parameter names are not available at runtime
 * (e.g. compiled without {@code -parameters}), {@link #entityParamIndex()}
 * can be used to point to the argument position explicitly.</p>
 *
 * <p>Example:</p>
 * <pre>
 * {@code @ValidateTenant(entityClass = Product.class)}
 * public void delete(UUID id) { ... }
 *
 * {@code @ValidateTenant(entityClass = Product.class, entityParam = "productId")}
 * public void update(UUID productId, UpdateRequest body) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateTenant {

  /**
   * Method parameter name that contains the entity identifier. Defaults to
   * {@code "id"} which matches the most common Spring MVC signatures.
   *
   * @return parameter name containing the entity identifier
   */
  String entityParam() default "id";

  /**
   * Optional fallback index of the argument that contains the entity
   * identifier. Use this when parameter names are not preserved in the
   * compiled bytecode.
   *
   * @return zero-based argument index, or -1 to ignore
   */
  int entityParamIndex() default -1;

  /**
   * Entity class that must be validated against the current tenant.
   *
   * @return JPA entity type
   */
  Class<?> entityClass();
}
