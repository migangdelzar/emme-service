package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.studio.subscriptions.api.PlanType;
import org.junit.jupiter.api.Test;

class SetPlatformFeatureFlagServiceTest {

  @Test
  void createsGlobalFlagWithItsPlanRequirement() {
    FeatureFlagTestRepository repository = new FeatureFlagTestRepository();
    SetPlatformFeatureFlagService service = new SetPlatformFeatureFlagService(repository);

    var result =
        service.set(new SetPlatformFeatureFlagCommand("calendar_sync", true, PlanType.ENTERPRISE));

    assertThat(result.code()).isEqualTo("calendar_sync");
    assertThat(result.enabled()).isTrue();
    assertThat(result.planRequired()).isEqualTo(PlanType.ENTERPRISE);
  }
}
