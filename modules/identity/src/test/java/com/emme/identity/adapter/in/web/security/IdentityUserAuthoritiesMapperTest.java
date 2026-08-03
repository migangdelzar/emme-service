package com.emme.identity.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

class IdentityUserAuthoritiesMapperTest {

  @Test
  void mapsRealmRolesFromOAuth2UserAttributes() {
    var mapper = new IdentityUserAuthoritiesMapper(new IdentityRoleAuthorityMapper());
    var authority =
        new OAuth2UserAuthority(
            "ROLE_USER", Map.of("realm_access", Map.of("roles", List.of("manager"))));

    assertThat(mapper.mapAuthorities(List.of(authority)))
        .extracting(value -> value.getAuthority())
        .contains("ROLE_USER", "ROLE_manager");
  }
}
