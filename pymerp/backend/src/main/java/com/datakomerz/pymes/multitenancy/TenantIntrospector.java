package com.datakomerz.pymes.multitenancy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that extracts the tenant identifier from managed entities. Supports
 * standard getters such as {@code getTenantId()} or {@code getCompanyId()} and
 * falls back to direct field access if necessary.
 */
final class TenantIntrospector {

  private static final Logger logger = LoggerFactory.getLogger(TenantIntrospector.class);
  private static final Map<Class<?>, Optional<Accessor>> ACCESSOR_CACHE = new ConcurrentHashMap<>();
  private static final String[] CANDIDATE_NAMES = {"tenantId", "companyId"};

  private TenantIntrospector() {
  }

  static UUID resolveTenantId(Object entity) {
    if (entity == null) {
      return null;
    }
    Optional<Accessor> accessorOptional =
        ACCESSOR_CACHE.computeIfAbsent(entity.getClass(), TenantIntrospector::locateAccessor);
    if (accessorOptional.isEmpty()) {
      return null;
    }
    try {
      Object value = accessorOptional.get().read(entity);
      if (value == null) {
        return null;
      }
      if (value instanceof UUID uuid) {
        return uuid;
      }
      if (value instanceof String str) {
        return UUID.fromString(str);
      }
      logger.warn("Unsupported tenant identifier type {} on {}", value.getClass(), entity.getClass().getName());
    } catch (Exception ex) {
      logger.warn("Failed to read tenant identifier from {}", entity.getClass().getName(), ex);
    }
    return null;
  }

  private static Optional<Accessor> locateAccessor(Class<?> type) {
    for (String candidate : CANDIDATE_NAMES) {
      // Try getter method first
      String getterName = "get" + Character.toUpperCase(candidate.charAt(0)) + candidate.substring(1);
      try {
        Method method = type.getMethod(getterName);
        if (method.getParameterCount() == 0) {
          method.setAccessible(true);
          return Optional.of(new MethodAccessor(method));
        }
      } catch (NoSuchMethodException ignored) {
        // continue searching
      }
      // Try public field access
      try {
        Field field = type.getDeclaredField(candidate);
        field.setAccessible(true);
        return Optional.of(new FieldAccessor(field));
      } catch (NoSuchFieldException ignored) {
        // continue searching
      }
    }
    logger.debug("No tenant accessor found for {}", type.getName());
    return Optional.empty();
  }

  private interface Accessor {
    Object read(Object target) throws Exception;
  }

  private record MethodAccessor(Method method) implements Accessor {
    @Override
    public Object read(Object target) throws Exception {
      return method.invoke(target);
    }
  }

  private record FieldAccessor(Field field) implements Accessor {
    @Override
    public Object read(Object target) throws IllegalAccessException {
      return field.get(target);
    }
  }
}
