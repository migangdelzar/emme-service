package com.emme.e2eprovisioner;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class E2eProvisionerApplicationTest {

  @Test
  void readsExistingTenantSlugsFromPlatformResponse() {
    assertThat(
            E2eProvisionerApplication.readTenantSlugs(
                "[{\"slug\":\"e2e-studio\"},{\"slug\":\"e2e-salon\"}]"))
        .containsExactlyInAnyOrder("e2e-studio", "e2e-salon");
  }

  @Test
  void platformControlPlaneRequestsDoNotForceTenantContext() {
    var request =
        E2eProvisionerApplication.platformRequest(URI.create("http://localhost:8081/api/tenants"))
            .GET()
            .build();

    assertThat(request.headers().firstValue("API-Version")).contains("1.0");
    assertThat(request.headers().firstValue("X-Tenant-Slug")).isEmpty();
  }
}
