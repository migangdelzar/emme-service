package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.FeatureFlag;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.studio.subscriptions.api.type.PlanType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeatureFlagEvaluationServiceTest {

  @Test
  void overlaysTenantOverridesOnGlobalDefaults() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.flags.add(new FeatureFlag(null, "calendar_sync", false, null, "Global default"));
    repository.flags.add(new FeatureFlag(tenantId, "calendar_sync", true, null, "Tenant override"));
    repository.flags.add(new FeatureFlag(null, "ai_chat", true, null, "Global default"));

    FeatureFlagEvaluationService service =
        new FeatureFlagEvaluationService(repository, ignored -> Optional.empty());

    assertThat(service.getEffective(tenantId))
        .containsEntry("calendar_sync", true)
        .containsEntry("ai_chat", true);
  }

  @Test
  void deniesAPlanRestrictedFlagWhenTheTenantPlanIsInsufficient() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.flags.add(new FeatureFlag(null, "ai_chat", true, PlanType.ENTERPRISE, "AI"));

    FeatureFlagEvaluationService service =
        new FeatureFlagEvaluationService(repository, ignored -> Optional.of(PlanType.STARTER));

    assertThat(TenantContextHolder.withTenantOverride(tenantId, () -> service.isEnabled("ai_chat")))
        .isFalse();
  }
}
