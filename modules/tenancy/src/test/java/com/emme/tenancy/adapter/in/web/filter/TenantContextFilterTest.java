package com.emme.tenancy.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    request.addHeader("X-Tenant-Slug", "another-tenant");
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
  void doesNotFallBackToAHeaderWhenTheAuthenticatedTenantRealmIsUnknown() throws Exception {
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
    request.addHeader("X-Tenant-Slug", "e2e-studio");
    var response = new MockHttpServletResponse();
    var chain =
        (jakarta.servlet.FilterChain)
            (ignoredRequest, ignoredResponse) ->
                assertThat(TenantContext.getCurrentTenantId()).isNull();

    filter.doFilter(request, response, chain);
  }
}
