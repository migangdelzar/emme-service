package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.E2eUsers;
import com.emme.client.Roles;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Roles.PLATFORM_ADMIN)
class IdentityApiTest {

  @Test
  void shouldGetCurrentUser(UserSession session) {
    var result = session.identity().me();
    assertThat(result).isNotNull();
  }

  @Test
  void shouldGetIdentityMe(UserSession session) {
    var result = session.identity().identityMe();
    assertThat(result).isNotNull();
  }

  @Test
  void shouldDenyFeatureFlagsWithoutAdmin(UserSession session) {
    var result = session.get("/api/admin/feature-flags", 403);
    assertThat(result).isNotNull();
  }

  @Test
  @WithUser(role = Roles.PLATFORM_ADMIN)
  @WithUser(role = Roles.TENANT_OWNER)
  void shouldInjectMultipleConfiguredUsers(E2eUsers users) {
    assertThat(users.size()).isEqualTo(2);
    assertThat(users.first().user().userId()).isNotEqualTo(users.get(1).user().userId());
  }

  @Test
  @WithUser(role = Roles.TENANT_OWNER)
  void shouldInjectMethodConfiguredUser(UserSession session) {
    assertThat(session.identity().me()).isNotNull();
  }
}
