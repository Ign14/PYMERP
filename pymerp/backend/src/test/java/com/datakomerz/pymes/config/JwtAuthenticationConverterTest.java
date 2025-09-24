package com.datakomerz.pymes.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationConverterTest {

  @Test
  void oidcRoleMapperExtractsRealmRoles() {
    Map<String, Object> claims = Map.of("realm_access", Map.of("roles", java.util.List.of("admin", "user")));
    Collection<GrantedAuthority> authorities = com.datakomerz.pymes.security.OidcRoleMapper.mapRolesFromClaims(claims);
    assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
  }
}
