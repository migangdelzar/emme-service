package com.emme.testing;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

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
 * Security via JWT claims. Tenant provisioning helpers belong to the tenancy-owned fixture.
 *
 * <p>Usage: {@code class AuthControllerTest extends BaseWebTest} Inherits: mockMvc field, auth()
 * helper, tenantId
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("web")
public abstract class BaseWebTest {

  @Autowired protected MockMvc mockMvc;

  protected UUID tenantId;

  /**
   * Build a JWT RequestPostProcessor with admin role and random tenant. Override in subclass to
   * customize roles or tenant.
   */
  protected JwtRequestPostProcessor auth() {
    if (tenantId == null) tenantId = UUID.randomUUID();
    return jwt()
        .jwt(
            j ->
                j.subject("test-user")
                    .issuer("https://issuer.example/emme-test")
                    .claim("tenant_id", tenantId.toString())
                    .claim("realm_access", Map.of("roles", List.of("admin"))))
        .authorities(new SimpleGrantedAuthority("admin"));
  }

  /** Build JWT with specific tenant and roles. */
  protected JwtRequestPostProcessor auth(UUID tenant, String... roles) {
    return jwt()
        .jwt(
            j ->
                j.subject("test-user")
                    .issuer("https://issuer.example/emme-test")
                    .claim("tenant_id", tenant.toString())
                    .claim("realm_access", Map.of("roles", List.of(roles))))
        .authorities(
            List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new));
  }
}
