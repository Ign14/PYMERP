package com.datakomerz.pymes.core.tenancy;
import org.springframework.lang.NonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class CompanyContextFilter extends OncePerRequestFilter {

  public static final String COMPANY_HEADER = "X-Company-Id";

  private final CompanyContext companyContext;

  public CompanyContextFilter(CompanyContext companyContext) {
    this.companyContext = companyContext;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
    throws ServletException, IOException {
    // Anotaciones @NonNull
    // ...existing code...

    try {
      String uri = request.getRequestURI();
      String headerValue = request.getHeader(COMPANY_HEADER);
      if (requiresCompanyHeader(uri) && !StringUtils.hasText(headerValue)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"missing_company_id\"}");
        return;
      }
      if (StringUtils.hasText(headerValue)) {
        UUID resolved = resolveCompanyId(headerValue);
        companyContext.set(resolved);
      }
      filterChain.doFilter(request, response);
    } catch (IllegalArgumentException ex) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"invalid_company_id\"}");
    } finally {
      companyContext.clear();
    }
  }

  private UUID resolveCompanyId(String headerValue) {
    if (!StringUtils.hasText(headerValue)) {
      throw new IllegalArgumentException("Company header missing");
    }
    return UUID.fromString(headerValue.trim());
  }

  private boolean requiresCompanyHeader(String uri) {
    if (uri == null) {
      return false;
    }
    if (uri.startsWith("/api/v1/requests")
        || uri.startsWith("/api/v1/auth")
        || uri.startsWith("/webhooks/")
        || uri.startsWith("/actuator/")) {
      return false;
    }
    return uri.startsWith("/api/");
  }
}
