package com.emme.e2eprovisioner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class E2eProvisionerApplicationTest {

  @Test
  void readsExistingTenantSlugsFromPlatformResponse() {
    assertThat(
            E2eProvisionerApplication.readTenantSlugs(
                "[{\"slug\":\"e2e-studio\"},{\"slug\":\"e2e-salon\"}]"))
        .containsExactlyInAnyOrder("e2e-studio", "e2e-salon");
  }
}
