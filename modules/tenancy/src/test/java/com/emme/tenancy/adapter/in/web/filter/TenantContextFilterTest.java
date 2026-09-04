package com.emme.tenancy.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import com.emme.kernel.context.TenantExecutionContextScope;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class TenantContextFilterTest {

  @AfterEach
  void clearSecurityContext() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  @Test
  void returnsGeneratedCorrelationIdToTheHttpCaller() throws Exception {
    var filter = new TenantContextFilter(mock(TenantRepository.class));
    var request = new MockHttpServletRequest("GET", "/api/health");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
  }

  @Test
  void resolvesTenantFromTheAuthenticatedTenantRealmBeforeRequestHeaders() throws Exception {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    UUID tenantId = UUID.randomUUID();
    Tenant tenant =
        Tenant.rehydrate(
            tenantId,
            "e2e-salon",
            "E2E Salon",
            TenantStatus.ACTIVE,
            null,
            "emme-e2e-salon",
            Instant.now(),
            Instant.now());
    when(tenantRepository.findByIdentityRealm("emme-e2e-salon")).thenReturn(Optional.of(tenant));
    when(tenantRepository.findDatabaseIdByTenantId(tenantId)).thenReturn(Optional.empty());

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://identity.example/realms/emme-e2e-salon")
            .subject("owner")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

    var filter = new TenantContextFilter(tenantRepository);
    var request = new MockHttpServletRequest("GET", "/api/ai/chat");
    var response = new MockHttpServletResponse();
    var chain =
        (jakarta.servlet.FilterChain)
            (ignoredRequest, ignoredResponse) -> {
              assertThat(TenantContext.getCurrentTenantId()).isEqualTo(tenantId);
              assertThat(TenantExecutionContextScope.requireCurrent().tenantId())
                  .isEqualTo(tenantId);
              assertThat(TenantExecutionContextScope.requireCurrent().databaseId()).isNull();
              assertThat(TenantExecutionContextScope.requireCurrent().correlationId())
                  .isEqualTo(response.getHeader(TenantContextFilter.CORRELATION_ID_HEADER));
            };

    filter.doFilter(request, response, chain);

    assertThat(TenantContext.getCurrentTenantId()).isNull();
    assertThat(TenantExecutionContextScope.current()).isEmpty();
  }

  @Test
  void rejectsAnAuthenticatedTenantRealmThatCannotBeResolved() {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    when(tenantRepository.findByIdentityRealm("emme-missing-salon")).thenReturn(Optional.empty());

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://identity.example/realms/emme-missing-salon")
            .subject("owner")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

    var filter = new TenantContextFilter(tenantRepository);
    var request = new MockHttpServletRequest("GET", "/api/ai/chat");
    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated tenant could not be resolved");
  }

  @Test
  void rejectsAnAuthenticatedTenantClaimThatIsMalformed() {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://identity.example/realms/emme-core")
            .subject("owner")
            .claim("tenant_id", "not-a-uuid")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

    var filter = new TenantContextFilter(tenantRepository);

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(
                        new MockHttpServletRequest("GET", "/api/ai/chat"),
                        new MockHttpServletResponse(),
                        new MockFilterChain())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Authenticated tenant could not be resolved");
  }

  @Test
  void failsClosedWhenDedicatedDatabaseLookupFails() throws Exception {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    UUID tenantId = UUID.randomUUID();
    Tenant tenant =
        Tenant.rehydrate(
            tenantId,
            "studio",
            "Studio",
            TenantStatus.ACTIVE,
            null,
            "emme-studio",
            Instant.now(),
            Instant.now());
    when(tenantRepository.findByIdentityRealm("emme-studio")).thenReturn(Optional.of(tenant));
    when(tenantRepository.findDatabaseIdByTenantId(tenantId))
        .thenThrow(new IllegalStateException("database registry unavailable"));

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://identity.example/realms/emme-studio")
            .subject("owner")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

    var filter = new TenantContextFilter(tenantRepository);
    var chain = mock(jakarta.servlet.FilterChain.class);

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(
                        new MockHttpServletRequest("GET", "/api/ai/chat"),
                        new MockHttpServletResponse(),
                        chain)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("database registry unavailable");
    verify(chain, never())
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsAnUnknownTenantSelectorBeforeSelectingATenant() {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    when(tenantRepository.findBySlug("missing")).thenReturn(Optional.empty());
    var filter = new TenantContextFilter(tenantRepository);
    var request = new MockHttpServletRequest("GET", "/api/ai/chat");
    request.addHeader("X-Tenant-Slug", " missing ");

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Unknown tenant selector");
  }

  @Test
  void rejectsDisagreeingTenantSelectorsBeforeSelectingATenant() {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    Tenant first =
        Tenant.rehydrate(
            firstId,
            "first",
            "First",
            TenantStatus.ACTIVE,
            null,
            "realm-first",
            Instant.now(),
            Instant.now());
    Tenant second =
        Tenant.rehydrate(
            secondId,
            "second",
            "Second",
            TenantStatus.ACTIVE,
            null,
            "realm-second",
            Instant.now(),
            Instant.now());
    when(tenantRepository.findBySlug("first")).thenReturn(Optional.of(first));
    when(tenantRepository.findBySlug("second")).thenReturn(Optional.of(second));
    var filter = new TenantContextFilter(tenantRepository);
    var request = new MockHttpServletRequest("GET", "/api/ai/chat");
    request.addHeader("X-Tenant-Slug", " first ");
    request.setParameter("tenant", "second");

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Tenant selectors disagree");
  }

  @Test
  void rejectsAValidCallerTenantSelectorThatConflictsWithTheAuthenticatedTenant() {
    TenantRepository tenantRepository = mock(TenantRepository.class);
    UUID authenticatedTenantId = UUID.randomUUID();
    UUID requestedTenantId = UUID.randomUUID();
    Tenant authenticatedTenant =
        Tenant.rehydrate(
            authenticatedTenantId,
            "authenticated-salon",
            "Authenticated Salon",
            TenantStatus.ACTIVE,
            null,
            "emme-authenticated-salon",
            Instant.now(),
            Instant.now());
    Tenant requestedTenant =
        Tenant.rehydrate(
            requestedTenantId,
            "requested-salon",
            "Requested Salon",
            TenantStatus.ACTIVE,
            null,
            "emme-requested-salon",
            Instant.now(),
            Instant.now());
    when(tenantRepository.findByIdentityRealm("emme-authenticated-salon"))
        .thenReturn(Optional.of(authenticatedTenant));
    when(tenantRepository.findBySlug("requested-salon")).thenReturn(Optional.of(requestedTenant));

    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://identity.example/realms/emme-authenticated-salon")
            .subject("owner")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

    var filter = new TenantContextFilter(tenantRepository);
    var request = new MockHttpServletRequest("GET", "/api/ai/chat");
    request.addHeader("X-Tenant-Slug", "requested-salon");

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Tenant selector conflicts with authenticated tenant");
  }
}
