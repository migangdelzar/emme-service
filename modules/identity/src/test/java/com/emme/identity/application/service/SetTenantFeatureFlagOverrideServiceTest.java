package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.application.support.FeatureFlagTestRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SetTenantFeatureFlagOverrideServiceTest {

  @Test
  void createsTenantOverrideWithTheRequestedState() {
    UUID tenantId = UUID.randomUUID();
    SetTenantFeatureFlagOverrideService service =
        new SetTenantFeatureFlagOverrideService(new FeatureFlagTestRepository());

    var result =
        service.set(new SetTenantFeatureFlagOverrideCommand(tenantId, "calendar_sync", true));

    assertThat(result.code()).isEqualTo("calendar_sync");
    assertThat(result.enabled()).isTrue();
  }
}
