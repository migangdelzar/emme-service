package com.emme.testing;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for L2 web slice tests (controller HTTP contracts). Provides MockMvc with full Spring
 * Security via JWT + tenant context.
 *
 * <p>Usage: {@code class AppointmentControllerTest extends BaseWebTest} Inherits: mockMvc field,
 * auth() helper, tenantId
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("web")
public abstract class BaseWebTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected CreateTenantUseCase createTenantUseCase;

  protected UUID tenantId;

  protected TenantInfo createTenant(String slug, String name) {
    return createTenantUseCase.create(new CreateTenantCommand(slug, name));
  }

  /**
   * Build a JWT RequestPostProcessor with platform_admin role and random tenant. Override in
   * subclass to customize roles or tenant.
   */
  protected JwtRequestPostProcessor auth() {
    if (tenantId == null) tenantId = UUID.randomUUID();
    return jwt()
        .jwt(
            j ->
                j.subject("test-user")
                    .claim("tenant_id", tenantId.toString())
                    .claim("realm_access", Map.of("roles", List.of("platform_admin"))))
        .authorities(new SimpleGrantedAuthority("platform_admin"));
  }

  /** Build JWT with specific tenant and roles. */
  protected JwtRequestPostProcessor auth(UUID tenant, String... roles) {
    return jwt()
        .jwt(
            j ->
                j.subject("test-user")
                    .claim("tenant_id", tenant.toString())
                    .claim("realm_access", Map.of("roles", List.of(roles))))
        .authorities(
            List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new));
  }
}
