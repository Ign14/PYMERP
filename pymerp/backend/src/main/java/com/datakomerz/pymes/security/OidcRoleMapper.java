package com.datakomerz.pymes.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility to extract roles and scopes from OIDC JWT claims.
 * - realm_access.roles -> ROLE_*
 * - resource_access[client].roles -> ROLE_*
 * - scope -> SCOPE_*
 */
public final class OidcRoleMapper {

  private static final Logger log = LoggerFactory.getLogger(OidcRoleMapper.class);

  private OidcRoleMapper() {}

  public static Collection<GrantedAuthority> mapRolesFromClaims(Map<String, Object> claims) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    if (claims == null) return authorities;

    // realm_access.roles
    Object realmAccess = claims.get("realm_access");
    if (realmAccess instanceof Map) {
      Object roles = ((Map<?, ?>) realmAccess).get("roles");
      addRoles(authorities, roles);
    }

    // top-level roles array
    Object topRoles = claims.get("roles");
    addRoles(authorities, topRoles);

    // resource_access.<client>.roles
    Object resourceAccess = claims.get("resource_access");
    if (resourceAccess instanceof Map) {
      Object clientObj = ((Map<?, ?>) resourceAccess).get("pymerp-backend");
      if (clientObj instanceof Map) {
        Object clientRoles = ((Map<?, ?>) clientObj).get("roles");
        addRoles(authorities, clientRoles);
      }
    }

    // scope -> SCOPE_*
    Object scope = claims.get("scope");
    if (scope instanceof String) {
      String scopes = (String) scope;
      for (String s : scopes.split(" ")) {
        s = s.trim();
        if (!s.isEmpty()) {
          authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
        }
      }
    }

    if (log.isDebugEnabled()) {
      List<String> names = new ArrayList<>();
      for (GrantedAuthority a : authorities) names.add(a.getAuthority());
      log.debug("Mapped authorities from JWT claims: {}", names);
    }

    return authorities;
  }

  private static void addRoles(Collection<GrantedAuthority> authorities, Object roles) {
    if (roles instanceof Iterable) {
      for (Object r : (Iterable<?>) roles) {
        if (r != null) {
          String normalized = normalizeRole(r.toString());
          authorities.add(new SimpleGrantedAuthority(normalized));
        }
      }
    }
  }

  private static String normalizeRole(String raw) {
    if (raw == null) return "";
    String cleaned = raw.replace('-', '_').replace(' ', '_').toUpperCase();
    if (cleaned.startsWith("ROLE_")) {
      return cleaned;
    }
    return "ROLE_" + cleaned;
  }
}
