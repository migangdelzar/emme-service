package com.emme.identity.configuration;

import com.emme.identity.adapter.in.web.filter.LoginRateLimitFilter;
import com.emme.identity.adapter.out.client.keycloak.MultiRealmJwtDecoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

  private final IdentitySecurityProperties securityProperties;

  public SecurityConfiguration(IdentitySecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(securityProperties.allowedOrigins());
    configuration.setAllowedMethods(securityProperties.allowedMethods());
    configuration.setAllowedHeaders(securityProperties.allowedHeaders());
    configuration.setAllowCredentials(securityProperties.allowCredentials());
    configuration.setMaxAge(securityProperties.maxAgeSeconds());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      LoginRateLimitFilter rateLimitFilter,
      MultiRealmJwtDecoder multiRealmJwtDecoder,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      GrantedAuthoritiesMapper userAuthoritiesMapper)
      throws Exception {
    http.securityMatcher(
            request -> {
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
                                    + "font-src 'self'; connect-src 'self' "
                                    + securityProperties.cspConnectSource()))
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
                    .requestMatchers("/api/webhooks/whatsapp/**")
                    .permitAll()

                    // Google OAuth callback — browser redirect from Google consent screen.
                    // Uses Keycloak session cookie, not JWT.
                    .requestMatchers("/api/google/oauth/callback")
                    .permitAll()

                    // Auth endpoints (login via local auth)
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/customer-login")
                    .permitAll()

                    // Tenant-level public catalog endpoints (shop-front)
                    .requestMatchers(HttpMethod.GET, "/api/tenants/*/services")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tenants/*/artists")
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
                        userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper)))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(
                        jwt ->
                            jwt.decoder(multiRealmJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .bearerTokenResolver(
                        request -> {
                          String uri = request.getRequestURI();
                          if (uri != null
                              && (uri.endsWith("/customer-login") || uri.endsWith("/login"))) {
                            return null;
                          }
                          String header = request.getHeader("Authorization");
                          return (header != null && header.startsWith("Bearer "))
                              ? header.substring(7)
                              : null;
                        }))
        .logout(
            logout ->
                logout
                    .logoutUrl("/oauth2/logout")
                    .logoutSuccessUrl(securityProperties.logoutSuccessUrl())
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
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }
}
