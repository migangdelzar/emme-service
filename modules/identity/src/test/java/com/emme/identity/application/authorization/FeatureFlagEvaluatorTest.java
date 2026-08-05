package com.emme.identity.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.application.support.FeatureFlagTestRepository;
import com.emme.identity.domain.model.FeatureFlag;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.subscriptions.api.type.PlanType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeatureFlagEvaluatorTest {

  @Test
  void overlaysTenantOverridesOnGlobalDefaults() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.flags.add(new FeatureFlag(null, "calendar_sync", false, null, "Global default"));
    repository.flags.add(new FeatureFlag(tenantId, "calendar_sync", true, null, "Tenant override"));
    repository.flags.add(new FeatureFlag(null, "ai_chat", true, null, "Global default"));

    FeatureFlagEvaluator evaluator =
        new FeatureFlagEvaluator(repository, ignored -> Optional.empty());

    assertThat(evaluator.getEffective(tenantId))
        .containsEntry("calendar_sync", true)
        .containsEntry("ai_chat", true);
  }

  @Test
  void deniesAPlanRestrictedFlagWhenTheTenantPlanIsInsufficient() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    repository.flags.add(new FeatureFlag(null, "ai_chat", true, PlanType.ENTERPRISE, "AI"));

    FeatureFlagEvaluator evaluator =
        new FeatureFlagEvaluator(repository, ignored -> Optional.of(PlanType.STARTER));

    assertThat(
            TenantContextHolder.withTenantOverride(tenantId, () -> evaluator.isEnabled("ai_chat")))
        .isFalse();
  }
}
