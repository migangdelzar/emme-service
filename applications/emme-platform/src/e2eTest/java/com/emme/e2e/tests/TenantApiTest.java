package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.client.E2eJson;
import org.junit.jupiter.api.Test;

class TenantApiTest {

  @Test
  void shouldCrudComplete() {
    withSession(
        s -> {
          var slug = "crud-" + System.currentTimeMillis();

          var created = s.tenants().create(slug, "CRUD Test Studio");
          assertThat(created).contains(slug).contains("ACTIVE");

          var list = s.tenants().list();
          assertThat(list).contains(slug);

          var id = E2eJson.extract(created, "id");
          var got = s.tenants().getById(id);
          assertThat(got).contains(slug);

          var updated = s.tenants().update(id, "Updated Studio");
          if (!updated.isEmpty()) assertThat(updated).contains("Updated Studio");
        });
  }

  @Test
  void shouldSetupFullStudio() {
    withSession(
        s -> {
          var tenantId = s.setup().fullStudio("full-" + System.currentTimeMillis());
          assertThat(tenantId).isNotNull();
        });
  }

  @Test
  void shouldRejectUnauthenticated() {
    withUnauthenticated(
        s -> {
          assertThatThrownBy(() -> s.tenants().list())
              .isInstanceOf(AssertionError.class)
              .hasMessageContaining("401");
        });
  }
}
