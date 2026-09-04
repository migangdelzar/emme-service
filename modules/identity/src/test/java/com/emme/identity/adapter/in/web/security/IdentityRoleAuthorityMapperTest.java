package com.emme.identity.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdentityRoleAuthorityMapperTest {

  private final IdentityRoleAuthorityMapper mapper = new IdentityRoleAuthorityMapper();

  @Test
  void mapsStringRealmRolesToPrefixedAuthorities() {
    var authorities =
        mapper.fromClaims(Map.of("realm_access", Map.of("roles", List.of("admin", "staff"))));

    assertThat(authorities)
        .extracting(authority -> authority.getAuthority())
        .containsExactlyInAnyOrder("ROLE_admin", "ROLE_staff");
  }

  @Test
  void ignoresMissingOrNonStringRealmRoles() {
    var authorities =
        mapper.fromClaims(Map.of("realm_access", Map.of("roles", List.of("staff", 42))));

    assertThat(authorities)
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_staff");
  }
}
