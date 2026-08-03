package com.emme.identity.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserContextHolderTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void currentSubjectReadsJwtSubject() {
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt(Map.of("sub", "user-1"))));

    assertThat(UserContextHolder.currentSubject()).isEqualTo("user-1");
  }

  @Test
  void currentEmailReadsJwtEmail() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt(Map.of("sub", "user-1", "email", "u@example.com"))));

    assertThat(UserContextHolder.currentEmail()).isEqualTo("u@example.com");
  }

  @Test
  void withCurrentUserPassesParsedUserContext() {
    UUID tenantId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt(
                    Map.of(
                        "sub", "user-1",
                        "email", "u@example.com",
                        "name", "User One",
                        "tenant_id", tenantId.toString()))));

    String result =
        UserContextHolder.withCurrentUser(
            user ->
                user.subject()
                    + "|"
                    + user.email()
                    + "|"
                    + user.displayName()
                    + "|"
                    + user.tenantId());

    assertThat(result).isEqualTo("user-1|u@example.com|User One|" + tenantId);
  }

  @Test
  void currentSubjectThrowsWhenNoAuthenticatedPrincipalExists() {
    assertThatThrownBy(UserContextHolder::currentSubject)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No authenticated user context");
  }

  @Test
  void rejectsTenantSelectionOutsideTheAuthenticatedTenant() {
    UUID currentTenantId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt(Map.of("sub", "user-1", "tenant_id", currentTenantId.toString()))));

    assertThatThrownBy(() -> UserContextHolder.requireTenantAccess(UUID.randomUUID()))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessage("Tenant access denied");
  }

  @Test
  void allowsPlatformAdministratorsToSelectAnotherTenant() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt(Map.of("sub", "admin")),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_platform_admin"))));

    UserContextHolder.requireTenantAccess(UUID.randomUUID());
  }

  private static Jwt jwt(Map<String, Object> claims) {
    return new Jwt(
        "token", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), Map.of("alg", "none"), claims);
  }
}
