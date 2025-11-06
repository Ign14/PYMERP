package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.metamodel.EntityType;
import java.util.Objects;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect that validates cross-tenant access before executing sensitive methods.
 *
 * <p>It fetches the entity referenced by the annotated method (using the
 * provided {@link ValidateTenant} metadata) and verifies that the entity
 * belongs to the tenant stored in {@link TenantContext}. If the entity exists
 * but belongs to another tenant, a {@link CrossTenantAccessException} is
 * raised.</p>
 */
@Aspect
@Component
public class TenantValidationAspect {

  private static final Logger logger = LoggerFactory.getLogger(TenantValidationAspect.class);

  private final EntityManager entityManager;
  private final TenantFilterEnabler filterEnabler;

  public TenantValidationAspect(EntityManager entityManager, TenantFilterEnabler filterEnabler) {
    this.entityManager = entityManager;
    this.filterEnabler = filterEnabler;
  }

  @Before("@annotation(validateTenant)")
  public void validateMethod(JoinPoint joinPoint, ValidateTenant validateTenant) {
    validate(joinPoint, validateTenant);
  }

  @Before("@within(validateTenant) && execution(* *(..))")
  public void validateType(JoinPoint joinPoint, ValidateTenant validateTenant) {
    validate(joinPoint, validateTenant);
  }

  private void validate(JoinPoint joinPoint, ValidateTenant metadata) {
    UUID currentTenant = TenantContext.require();

    Object rawId = resolveEntityId(joinPoint, metadata);
    if (rawId == null) {
      logger.debug("Skip tenant validation for {} - entity id not resolved", joinPoint.getSignature());
      return;
    }

    Object entityId = convertIdentifier(rawId, metadata.entityClass());

    // With filter enabled we should only see entities for the current tenant.
    filterEnabler.enableTenantFilter(entityManager);
    Object entity = entityManager.find(metadata.entityClass(), entityId);
    if (entity != null) {
      ensureTenantMatch(metadata.entityClass(), entityId, currentTenant, entity);
      return;
    }

    // Not visible with tenant filter -> check if it exists in another tenant.
    try {
      filterEnabler.disableTenantFilter(entityManager);
      Object entityWithoutFilter = entityManager.find(metadata.entityClass(), entityId);
      if (entityWithoutFilter == null) {
        throw new EntityNotFoundException(metadata.entityClass().getSimpleName() + " not found");
      }
      try {
        ensureTenantMatch(metadata.entityClass(), entityId, currentTenant, entityWithoutFilter);
      } finally {
        if (entityManager.contains(entityWithoutFilter)) {
          entityManager.detach(entityWithoutFilter);
        }
      }
    } finally {
      filterEnabler.enableTenantFilter(entityManager);
    }
  }

  private void ensureTenantMatch(Class<?> entityClass,
                                 Object entityId,
                                 UUID currentTenant,
                                 Object entity) {
    UUID entityTenant = TenantIntrospector.resolveTenantId(entity);

    if (entityTenant == null) {
      throw new IllegalStateException("Entity " + entityClass.getSimpleName()
          + " must expose a tenant identifier (companyId or tenantId)");
    }

    if (!Objects.equals(currentTenant, entityTenant)) {
      throw new CrossTenantAccessException(
          "Access denied: " + entityClass.getSimpleName() + "#" + entityId
              + " belongs to tenant " + entityTenant);
    }
  }

  private Object resolveEntityId(JoinPoint joinPoint, ValidateTenant metadata) {
    Object[] args = joinPoint.getArgs();
    if (args == null || args.length == 0) {
      return null;
    }

    int index = metadata.entityParamIndex();
    if (index >= 0 && index < args.length) {
      return args[index];
    }

    String expectedName = metadata.entityParam();
    if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
      return null;
    }
    String[] parameterNames = signature.getParameterNames();
    if (parameterNames == null) {
      logger.warn("Parameter names not available for {}", signature.toShortString());
      return null;
    }
    for (int i = 0; i < parameterNames.length; i++) {
      if (expectedName.equals(parameterNames[i]) && i < args.length) {
        return args[i];
      }
    }
    logger.warn("Could not find parameter '{}' on {}", expectedName, signature.toShortString());
    return null;
  }

  private Object convertIdentifier(Object rawId, Class<?> entityClass) {
    if (rawId == null) {
      return null;
    }

    Class<?> idType = resolveIdType(entityClass);

    if (idType.isInstance(rawId)) {
      return rawId;
    }

    if (UUID.class.equals(idType)) {
      if (rawId instanceof UUID uuid) {
        return uuid;
      }
      if (rawId instanceof String str && !str.isBlank()) {
        return UUID.fromString(str.trim());
      }
    } else if (String.class.equals(idType)) {
      return rawId.toString();
    } else if (Long.class.equals(idType) || long.class.equals(idType)) {
      if (rawId instanceof Number number) {
        return number.longValue();
      }
      if (rawId instanceof String str && !str.isBlank()) {
        return Long.parseLong(str.trim());
      }
    } else if (Integer.class.equals(idType) || int.class.equals(idType)) {
      if (rawId instanceof Number number) {
        return number.intValue();
      }
      if (rawId instanceof String str && !str.isBlank()) {
        return Integer.parseInt(str.trim());
      }
    }

    throw new IllegalArgumentException(
        "Unsupported identifier conversion for type " + idType.getName()
            + " using value " + rawId);
  }

  private Class<?> resolveIdType(Class<?> entityClass) {
    EntityType<?> entityType = entityManager.getMetamodel().entity(entityClass);
    return entityType.getIdType().getJavaType();
  }
}
