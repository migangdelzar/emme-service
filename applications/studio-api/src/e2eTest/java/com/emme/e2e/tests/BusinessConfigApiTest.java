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
class BusinessConfigApiTest {
  @Test
  void shouldGetConfig(UserSession api) {
    var result = api.get("/api/v1/business-config", 404);
    assertThat(result).isNotNull();
  }
}
