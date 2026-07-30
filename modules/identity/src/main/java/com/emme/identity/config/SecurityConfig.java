package com.emme.identity.config;

import com.emme.identity.infrastructure.LoginRateLimitFilter;
import com.emme.identity.infrastructure.MultiRealmJwtDecoder;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8100",
            "capacitor://localhost"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, LoginRateLimitFilter rateLimitFilter,
      MultiRealmJwtDecoder multiRealmJwtDecoder) throws Exception {
    http.securityMatcher(request -> {
          String uri = request.getRequestURI();
          return !uri.startsWith("/api/auth/customer-login");
        })
        .headers(
            headers ->
                headers
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .requestMatcher(request -> true))
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                                    + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                                    + "font-src 'self'; connect-src 'self' http://localhost:*"))
                    .frameOptions(frame -> frame.deny())
                    .xssProtection(xss -> xss.disable())
                    .contentTypeOptions(contentType -> contentType.disable()))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**", "/graphql"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/info")
                    .permitAll()
                    .requestMatchers("/api-docs/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**")
                    .permitAll()

                    // Meta WhatsApp webhook — verification + incoming messages (unauthenticated)
                    .requestMatchers("/api/v1/webhooks/whatsapp/**")
                    .permitAll()

                    // Google OAuth callback — browser redirect from Google consent screen.
                    // Uses Keycloak session cookie, not JWT.
                    .requestMatchers("/api/v1/google/oauth/callback")
                    .permitAll()

                    // Auth endpoints (login via local auth)
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/customer-login")
                    .permitAll()

                    // Tenant-level public catalog endpoints (shop-front)
                    .requestMatchers(HttpMethod.GET, "/api/v1/tenants/*/services")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/tenants/*/artists")
                    .permitAll()

                    // Everything else requires authentication
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .loginPage("/oauth2/authorization/keycloak")
                    .defaultSuccessUrl("http://localhost:3000/#/dashboard", true)
                    .userInfoEndpoint(
                        userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper())))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt
                    .decoder(multiRealmJwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .bearerTokenResolver(request -> {
                    String uri = request.getRequestURI();
                    if (uri != null && (uri.endsWith("/customer-login") || uri.endsWith("/login"))) {
                        return null;
                    }
                    String header = request.getHeader("Authorization");
                    return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
                }))
        .logout(
            logout ->
                logout
                    .logoutUrl("/oauth2/logout")
                    .logoutSuccessUrl(
                        "http://localhost:18080/realms/emme/protocol/openid-connect/logout"
                            + "?redirect_uri=http://localhost:3000")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                        (request, response, exception) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write(
                                  """
                  {"type":"about:blank","title":"Forbidden","status":403,"detail":"Access denied"}""");
                        })
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write(
                                  """
                  {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication required"}""");
                        }));

    return http.build();
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain publicAuthFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/auth/customer-login")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Set<GrantedAuthority> authorities = new HashSet<>();
          Object realmAccess = jwt.getClaims().get("realm_access");
          if (realmAccess instanceof Map<?, ?> ra) {
            Object roles = ra.get("roles");
            if (roles instanceof Collection<?> roleList) {
              for (Object role : roleList) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
              }
            }
          }
          return authorities;
        });
    return converter;
  }

  /**
   * Maps OAuth2/OIDC user attributes to Spring Security authorities. For OAuth2 login
   * (session-based), reads realm_access.roles from the ID token or user info claims and prefixes
   * them with ROLE_.
   */
  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) -> {
      Set<GrantedAuthority> mapped = new HashSet<>(authorities);

      for (GrantedAuthority authority : authorities) {
        if (authority instanceof OidcUserAuthority oidc) {
          Map<String, Object> attrs = oidc.getAttributes();
          extractRealmRoles(attrs, mapped);
          // Also check userInfo claims
          @SuppressWarnings("unchecked")
          Map<String, Object> userInfo = (Map<String, Object>) attrs.get("userinfo");
          if (userInfo != null) {
            extractRealmRoles(userInfo, mapped);
          }
        } else if (authority instanceof OAuth2UserAuthority oauth2) {
          extractRealmRoles(oauth2.getAttributes(), mapped);
        }
      }

      return mapped;
    };
  }

  @SuppressWarnings("unchecked")
  private static void extractRealmRoles(Map<String, Object> claims, Set<GrantedAuthority> target) {
    Object realmAccess = claims.get("realm_access");
    if (realmAccess instanceof Map<?, ?> ra) {
      Object roles = ra.get("roles");
      if (roles instanceof Collection<?> roleList) {
        for (Object role : roleList) {
          target.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
        }
      }
    }
  }
}
