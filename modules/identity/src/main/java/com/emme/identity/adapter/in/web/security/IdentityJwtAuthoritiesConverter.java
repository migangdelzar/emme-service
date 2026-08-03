package com.emme.identity.adapter.in.web.security;

import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Converts JWT realm roles into Spring Security authorities. */
@Component
public final class IdentityJwtAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  private final IdentityRoleAuthorityMapper roleAuthorityMapper;

  public IdentityJwtAuthoritiesConverter(IdentityRoleAuthorityMapper roleAuthorityMapper) {
    this.roleAuthorityMapper = roleAuthorityMapper;
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    return roleAuthorityMapper.fromClaims(jwt.getClaims());
  }
}
