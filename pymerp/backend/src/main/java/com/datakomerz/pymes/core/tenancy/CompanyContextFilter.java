package com.datakomerz.pymes.core.tenancy;
import org.springframework.lang.NonNull;

import com.datakomerz.pymes.config.AppProperties;
import com.datakomerz.pymes.security.AppUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class CompanyContextFilter extends OncePerRequestFilter {

  public static final String COMPANY_HEADER = "X-Company-Id";

  private final CompanyContext companyContext;
  private final AppProperties appProperties;

  public CompanyContextFilter(CompanyContext companyContext, AppProperties appProperties) {
    this.companyContext = companyContext;
    this.appProperties = appProperties;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
    throws ServletException, IOException {
    // Anotaciones @NonNull
    // ...existing code...

    try {
      String uri = request.getRequestURI();
      String headerValue = request.getHeader(COMPANY_HEADER);
      UUID resolved = resolveCompanyId(headerValue);
      // If request targets protected API endpoints and header is missing -> reject
      if (uri != null && uri.startsWith("/api/") && (headerValue == null || headerValue.isBlank())) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"missing_company_id\"}");
        return;
      }
      if (resolved != null) {
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
    if (StringUtils.hasText(headerValue)) {
      return UUID.fromString(headerValue.trim());
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails userDetails) {
      return userDetails.getCompanyId();
    }
    return appProperties.getTenancy().getDefaultCompanyId();
  }
}
