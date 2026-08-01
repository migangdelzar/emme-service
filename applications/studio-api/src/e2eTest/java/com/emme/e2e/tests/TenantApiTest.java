package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class TenantApiTest {

  private static final String DEMO = "00000000-0000-0000-0000-100000000000";

  @Test
  void shouldCrudComplete(UserSession api) {
    api.setup().subscription(DEMO);
    var slug = "e2e-" + System.nanoTime();
    var body = api.tenants().create(slug, "E2E CRUD Corp");
    assertThat(body).contains(slug);
    var list = api.tenants().list();
    assertThat(list).startsWith("[");
  }

  @Test
  void shouldSetupFullStudio(UserSession api) {
    var slug = "e2e-studio-" + (System.nanoTime() % 100000);
    api.setup().fullStudio(slug);
    assertThat(api.tenants().list()).contains(slug);
  }
}
