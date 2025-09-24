package com.datakomerz.pymes.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

public class OidcRoleMapperTest {

  @Test
  void mapsRealmRolesAndScopesAndResourceAccessRoles() {
    Map<String, Object> claims = Map.of(
      "realm_access", Map.of("roles", java.util.List.of("erp_user")),
      "resource_access", Map.of("pymerp-backend", Map.of("roles", java.util.List.of("erp_user"))),
      "scope", "products:read other:scope"
    );
    Collection<GrantedAuthority> authorities = OidcRoleMapper.mapRolesFromClaims(claims);
    assertThat(authorities).extracting(GrantedAuthority::getAuthority)
      .contains("ROLE_ERP_USER", "SCOPE_products:read", "SCOPE_other:scope");
  }
}
