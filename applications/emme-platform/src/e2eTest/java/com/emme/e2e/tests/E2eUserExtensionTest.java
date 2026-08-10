package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Roles;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Verifies method-scoped identity configuration for the canonical E2E extension. */
@ExtendWith(E2eUserExtension.class)
class E2eUserExtensionTest {

  @Test
  @WithUser(role = Roles.TENANT_OWNER)
  void shouldProvisionAUserDeclaredOnlyOnTheTestMethod(UserSession session) {
    assertThat(session.identity().me()).isNotNull();
  }
}
