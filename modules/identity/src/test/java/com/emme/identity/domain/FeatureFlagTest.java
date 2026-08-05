package com.emme.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.FeatureFlag;
import com.emme.subscriptions.api.type.PlanType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeatureFlagTest {

  @Test
  void changesEnabledStateWithoutChangingItsIdentityOrPlanRequirement() {
    UUID tenantId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(tenantId, "calendar_sync", false, PlanType.PRO, "Calendar access");

    flag.changeEnabled(true);

    assertThat(flag.tenantId()).isEqualTo(tenantId);
    assertThat(flag.code()).isEqualTo("calendar_sync");
    assertThat(flag.isEnabled()).isTrue();
    assertThat(flag.planRequired()).isEqualTo(PlanType.PRO);
  }
}
