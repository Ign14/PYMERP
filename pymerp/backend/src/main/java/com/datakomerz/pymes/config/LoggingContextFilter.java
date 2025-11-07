package com.datakomerz.pymes.config;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter que enriquece el MDC (Mapped Diagnostic Context) con información de contexto
 * para cada request HTTP. Permite tracking distribuido y correlación de logs.
 *
 * <p>MDC fields populated:
 * <ul>
 *   <li>traceId: UUID único por request para tracking distribuido</li>
 *   <li>userId: Username del usuario autenticado (si existe)</li>
 *   <li>companyId: ID de la compañía en contexto multitenancy (si existe)</li>
 *   <li>requestUri: URI del request HTTP</li>
 *   <li>method: Método HTTP (GET, POST, etc.)</li>
 * </ul>
 *
 * <p>El traceId también se retorna en el header HTTP "X-Trace-ID" para debugging.
 */
@Component
public class LoggingContextFilter extends OncePerRequestFilter {

    private final CompanyContext companyContext;

    public LoggingContextFilter(CompanyContext companyContext) {
        this.companyContext = companyContext;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Generar trace ID único para tracking de request
            String traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);

            // Usuario autenticado (si existe)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                MDC.put("userId", auth.getName());
            }

            // Company ID para multitenancy
            companyContext.current().ifPresent(companyId -> 
                MDC.put("companyId", companyId.toString())
            );

            // Request metadata
            MDC.put("requestUri", request.getRequestURI());
            MDC.put("method", request.getMethod());

            // Agregar trace ID al response header para debugging
            response.setHeader("X-Trace-ID", traceId);

            filterChain.doFilter(request, response);
        } finally {
            // Limpiar MDC para evitar memory leaks en thread pools
            MDC.clear();
        }
    }
}
