package com.emme.identity.adapter.in.web.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/** Maps Keycloak realm roles from trusted token claims to Spring authorities. */
@Component
public final class IdentityRoleAuthorityMapper {

  public Set<GrantedAuthority> fromClaims(Map<String, ?> claims) {
    Object realmAccess = claims.get("realm_access");
    if (!(realmAccess instanceof Map<?, ?> realmClaims)) {
      return Set.of();
    }

    Object roles = realmClaims.get("roles");
    if (!(roles instanceof Collection<?> roleValues)) {
      return Set.of();
    }

    return roleValues.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }
}
