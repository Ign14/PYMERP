package com.datakomerz.pymes.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts the tenant id from the X-Company-Id header and stores it in
 * {@link TenantContext}.
 *
 * <p>Routes listed in {@link #EXCLUDED_PATHS} are considered public and do not require the header.</p>
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

  public static final String TENANT_HEADER = "X-Company-Id";

  private static final List<String> EXCLUDED_PATHS = List.of(
      "/api/v1/auth/login",
      "/api/v1/auth/register",
      "/api/v1/auth/refresh",
      "/api/v1/auth/request-account",
      "/actuator/**",
      "/error"
  );

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {

    String path = request.getRequestURI();

    if (isExcludedPath(path)) {
      logger.debug("Public path, no tenant required: {}", path);
      return true;
    }

    String tenantIdHeader = request.getHeader(TENANT_HEADER);
    if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
      logger.warn("Missing {} header for path: {}", TENANT_HEADER, path);
      throw new TenantNotFoundException(
          "Header " + TENANT_HEADER + " is required for this endpoint");
    }

    try {
      UUID tenantId = UUID.fromString(tenantIdHeader);
      TenantContext.setTenantId(tenantId);
      logger.debug("Tenant context set: {} for path: {}", tenantId, path);
      return true;
    } catch (IllegalArgumentException ex) {
      logger.warn("Invalid {} format: {} for path: {}", TENANT_HEADER, tenantIdHeader, path);
      throw new TenantNotFoundException(
          "Invalid " + TENANT_HEADER + " format. Expected UUID, got: " + tenantIdHeader, ex);
    }
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex) {

    UUID tenantId = TenantContext.getTenantId();
    if (tenantId != null) {
      logger.debug("Clearing tenant context: {}", tenantId);
      TenantContext.clear();
    }
  }

  private boolean isExcludedPath(String path) {
    return EXCLUDED_PATHS.stream().anyMatch(pattern -> {
      if (pattern.endsWith("/**")) {
        String basePath = pattern.substring(0, pattern.length() - 3);
        return path.equals(basePath) || path.startsWith(basePath + "/");
      }
      return path.equals(pattern) || path.startsWith(pattern + "/");
    });
  }
}
