package com.emme.identity.configuration;

import com.emme.identity.adapter.in.web.security.IdentityJwtAuthoritiesConverter;
import com.emme.identity.adapter.in.web.security.IdentityUserAuthoritiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** Authorization policy and token-authority wiring owned by Identity. */
@Configuration
public class IdentityAuthorizationConfiguration {

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        """
        ROLE_platform_admin > ROLE_tenant_owner
        ROLE_tenant_owner > ROLE_business_owner
        ROLE_business_owner > ROLE_manager
        ROLE_manager > ROLE_staff
    """);
  }

  @Bean
  @SuppressWarnings("deprecation")
  static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
      RoleHierarchy roleHierarchy) {
    var handler = new DefaultMethodSecurityExpressionHandler();
    handler.setRoleHierarchy(roleHierarchy);
    return handler;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter(
      IdentityJwtAuthoritiesConverter authoritiesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }

  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapper(
      IdentityUserAuthoritiesMapper authoritiesMapper) {
    return authoritiesMapper;
  }
}
