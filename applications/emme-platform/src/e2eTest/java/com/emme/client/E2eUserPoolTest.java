package com.emme.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class E2eUserPoolTest {

  @Test
  void initializesUsersForRoleFilteredAcquisition() {
    var user = E2eUserPool.INSTANCE.acquire(Roles.TENANT_OWNER);

    try {
      assertThat(user.roles()).contains(Roles.TENANT_OWNER);
    } finally {
      E2eUserPool.INSTANCE.release(user.userId());
    }
  }
}
