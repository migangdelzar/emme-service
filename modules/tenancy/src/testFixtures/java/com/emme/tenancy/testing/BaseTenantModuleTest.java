package com.emme.tenancy.testing;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.adapter.out.persistence.repository.SpringDataFeatureFlagRepository;
import com.emme.identity.testing.MockIdentityProviderAdministrationConfig;
import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import com.emme.testing.TestBootstrapJdbcConfig;
import com.emme.testing.TestSecurityConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  @Autowired protected SpringDataSubscriptionRepository subscriptionRepo;
  @Autowired protected SpringDataFeatureFlagRepository featureFlagRepo;

  @Autowired
  protected com.emme.identity.adapter.out.persistence.repository.SpringDataMembershipRepository
      membershipRepo;

  @Autowired
  protected com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository roleRepo;

  protected static final String TEST_USER_SUB = "auth0|test-user-123";
  protected static final String TEST_ISSUER = "https://test-issuer/realms/emme";
  protected UUID tenantId;

  /** Create tenant + ENTERPRISE subscription + global feature flags. */
  protected UUID fullSetup() {
    TenantDetails tenant = createTenant("test-" + System.nanoTime(), "Test Salon");
    UUID tid = tenant.id();
    tenantId = tid;

    if (subscriptionRepo.findAll().stream()
        .noneMatch(subscription -> tid.equals(subscription.getTenantId()))) {
      subscriptionRepo.save(
          new SubscriptionEntity(
              tid, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
    }

    String[] flags = {
      "ai_chat",
      "analytics_export",
      "calendar_sync",
      "whatsapp_booking",
      "google_workspace",
      "google_sheets_export",
      "client_google_sync"
    };
    for (String code : flags) {
      if (featureFlagRepo.findByTenantIdIsNull().stream()
          .noneMatch(f -> f.getCode().equals(code))) {
        featureFlagRepo.save(new FeatureFlagEntity(null, code, true, null, "global default"));
      }
    }
    return tid;
  }

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
