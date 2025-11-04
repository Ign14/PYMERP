package com.datakomerz.pymes.inventory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditContextService {

  /**
   * Obtiene el email/username del usuario autenticado
   */
  public String getCurrentUser() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
          return (String) principal;
        }
        return authentication.getName();
      }
    } catch (Exception e) {
      // En caso de error, retornar sistema
    }
    return "SYSTEM";
  }

  /**
   * Obtiene la dirección IP del cliente
   */
  public String getUserIp() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
          ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
          ip = request.getRemoteAddr();
        }
        return ip;
      }
    } catch (Exception e) {
      // En caso de error, retornar null
    }
    return null;
  }
}
