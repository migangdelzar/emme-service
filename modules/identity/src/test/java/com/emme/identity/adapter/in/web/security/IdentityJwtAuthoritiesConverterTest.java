package com.emme.identity.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IdentityJwtAuthoritiesConverterTest {

  @Test
  void convertsJwtRealmRolesWithoutExposingClaimParsingToSecurityConfiguration() {
    var converter = new IdentityJwtAuthoritiesConverter(new IdentityRoleAuthorityMapper());
    var jwt =
        new Jwt(
            "token",
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(60),
            Map.of("alg", "none"),
            Map.of("realm_access", Map.of("roles", List.of("tenant_owner"))));

    assertThat(converter.convert(jwt))
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_tenant_owner");
  }
}
