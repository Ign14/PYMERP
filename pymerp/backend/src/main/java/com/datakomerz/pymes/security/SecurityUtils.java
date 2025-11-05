package com.datakomerz.pymes.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Utilidades para obtener información del usuario autenticado desde SecurityContext.
 */
public class SecurityUtils {

  private SecurityUtils() {
    // Utility class
  }

  /**
   * Obtiene el username del usuario autenticado.
   * @return Optional con el username, vacío si no está autenticado
   */
  public static Optional<String> getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    // Si es JWT de Keycloak, el nombre puede estar en "preferred_username"
    if (authentication.getPrincipal() instanceof Jwt jwt) {
      String preferredUsername = jwt.getClaimAsString("preferred_username");
      if (preferredUsername != null) {
        return Optional.of(preferredUsername);
      }
      return Optional.of(jwt.getSubject());
    }

    return Optional.ofNullable(authentication.getName());
  }

  /**
   * Obtiene los roles del usuario autenticado.
   * @return Lista de roles (ROLE_ADMIN, ROLE_ERP_USER, etc.)
   */
  public static List<String> getCurrentUserRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return List.of();
    }

    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  /**
   * Obtiene el Company ID del usuario autenticado desde el claim "company_id" del JWT.
   * @return Optional con el company ID, vacío si no está presente
   */
  public static Optional<Long> getCurrentUserCompanyId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Optional.empty();
    }

    if (authentication.getPrincipal() instanceof Jwt jwt) {
      Object companyId = jwt.getClaim("company_id");
      if (companyId instanceof Number number) {
        return Optional.of(number.longValue());
      }
      if (companyId instanceof String str) {
        try {
          return Optional.of(Long.parseLong(str));
        } catch (NumberFormatException e) {
          return Optional.empty();
        }
      }
    }

    return Optional.empty();
  }
}
