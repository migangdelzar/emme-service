package com.emme.tenancy.testing;

import com.emme.identity.testing.MockIdentityProviderAdministrationConfig;
import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import com.emme.testing.TestBootstrapJdbcConfig;
import com.emme.testing.TestSecurityConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Base class for full-context module tests that need tenant-owned setup. */
@SpringBootTest(
    classes = com.emme.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import({
  TestSecurityConfig.class,
  TestBootstrapJdbcConfig.class,
  MockIdentityProviderAdministrationConfig.class
})
@ActiveProfiles("test")
public abstract class BaseTenantModuleTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected CreateTenantUseCase createTenantUseCase;
  @Autowired protected GetTenantUseCase getTenantUseCase;
  protected static final String TEST_USER_SUB = "auth0|test-user-123";
  protected static final String TEST_ISSUER = "https://test-issuer/realms/emme";
  protected UUID tenantId;

  protected TenantDetails createTenant(String slug, String name) {
    return createTenantUseCase.create(new CreateTenantCommand(slug, name));
  }

  protected TenantDetails findTenant(UUID tenantId) {
    return getTenantUseCase.get(new GetTenantQuery(tenantId)).orElseThrow();
  }

  /** JWT with admin + tenant_owner roles. */
  protected RequestPostProcessor tenantJwt() {
    return tenantJwt(tenantId, TEST_USER_SUB, "admin", "tenant_owner");
  }

  protected RequestPostProcessor tenantJwt(UUID tid, String sub, String... roles) {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(
            Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer(TEST_ISSUER)
                .subject(sub)
                .claim("tenant_id", tid.toString())
                .claim("sub", sub)
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build())
        .authorities(
            List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toArray(org.springframework.security.core.GrantedAuthority[]::new));
  }
}
