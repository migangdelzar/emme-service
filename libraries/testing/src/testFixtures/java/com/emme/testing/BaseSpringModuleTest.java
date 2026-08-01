package com.emme.testing;

import com.emme.identity.entity.FeatureFlag;
import com.emme.identity.entity.FeatureFlagRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.entity.Subscription;
import com.emme.studio.subscriptions.entity.SubscriptionRepository;
import com.emme.tenancy.application.TenantService;
import com.emme.tenancy.entity.Tenant;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

/**
 * Base class for L4 module tests (full Spring context with H2, no Docker). Provides MockMvc, tenant
 * setup, JWT auth, and cleanup.
 *
 * <p>Runs in {@code src/test} on every build. No Docker required.
 *
 * <p>Usage: {@code class SalonApiTest extends BaseSpringModuleTest}
 */
@SpringBootTest(
    classes = com.emme.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, MockKeycloakAdminClientConfig.class})
@ActiveProfiles("test")
public abstract class BaseSpringModuleTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected TenantService tenantService;
  @Autowired protected SubscriptionRepository subscriptionRepo;
  @Autowired protected FeatureFlagRepository featureFlagRepo;
  @Autowired protected SpringDataBusinessProfileRepository profileRepo;
  @Autowired protected com.emme.identity.entity.MembershipRepository membershipRepo;
  @Autowired protected com.emme.identity.entity.RoleRepository roleRepo;

  protected static final String TEST_USER_SUB = "auth0|test-user-123";
  protected static final String TEST_ISSUER = "https://test-issuer/realms/emme";
  protected UUID tenantId;

  /** Create tenant + ENTERPRISE subscription + global feature flags. */
  protected UUID fullSetup() {
    Tenant tenant = tenantService.create("test-" + System.nanoTime(), "Test Salon");
    UUID tid = tenant.getId();
    tenantId = tid;

    if (subscriptionRepo.findByTenantId(tid).isEmpty()) {
      subscriptionRepo.save(
          new Subscription(tid, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
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
        featureFlagRepo.save(new FeatureFlag(null, code, true, null, "global default"));
      }
    }
    return tid;
  }

  /** JWT with platform_admin + tenant_owner roles. */
  protected RequestPostProcessor tenantJwt() {
    return tenantJwt(tenantId, TEST_USER_SUB, "platform_admin", "tenant_owner");
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

  @AfterEach
  protected void baseTearDown() {
    /* override in subclass */
  }
}
