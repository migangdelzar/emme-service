package com.emme.identity.adapter.in.web.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

/** Maps Keycloak realm roles from browser OAuth2/OIDC authorities. */
@Component
public final class IdentityUserAuthoritiesMapper implements GrantedAuthoritiesMapper {

  private final IdentityRoleAuthorityMapper roleAuthorityMapper;

  public IdentityUserAuthoritiesMapper(IdentityRoleAuthorityMapper roleAuthorityMapper) {
    this.roleAuthorityMapper = roleAuthorityMapper;
  }

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    Set<GrantedAuthority> mapped = new HashSet<>(authorities);

    for (GrantedAuthority authority : authorities) {
      if (authority instanceof OidcUserAuthority oidc) {
        Map<String, Object> attributes = oidc.getAttributes();
        mapped.addAll(roleAuthorityMapper.fromClaims(attributes));
        mapped.addAll(roleAuthorityMapper.fromClaims(asStringKeyedMap(attributes.get("userinfo"))));
      } else if (authority instanceof OAuth2UserAuthority oauth2) {
        mapped.addAll(roleAuthorityMapper.fromClaims(oauth2.getAttributes()));
      }
    }

    return mapped;
  }

  private static Map<String, Object> asStringKeyedMap(Object value) {
    if (!(value instanceof Map<?, ?> rawMap)) {
      return Map.of();
    }
    return rawMap.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof String)
        .collect(
            Collectors.toUnmodifiableMap(entry -> (String) entry.getKey(), Map.Entry::getValue));
  }
}
