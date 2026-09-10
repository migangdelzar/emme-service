package com.emme.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class E2eUserSpecTest {

  @Test
  void describesOneUserWithMultipleRolesWithoutTokenEnvironmentNames() {
    var specification = E2eUserSpec.authenticated(Roles.PLATFORM_ADMIN, Roles.TENANT_OWNER);

    assertThat(specification.roles()).containsExactly(Roles.PLATFORM_ADMIN, Roles.TENANT_OWNER);
    assertThat(specification.accessToken()).isEmpty();
    assertThat(specification.authenticated()).isTrue();
  }

  @Test
  void copiesRoleInputAndNormalizesNullableOptionalValues() {
    var roles = new java.util.ArrayList<>(List.of(Roles.TENANT_OWNER));
    var specification = new E2eUserSpec(roles, null, null, true);
    roles.add(Roles.PLATFORM_ADMIN);

    assertThat(specification.roles()).containsExactly(Roles.TENANT_OWNER);
    assertThat(specification.tenantId()).isEmpty();
    assertThat(specification.accessToken()).isEmpty();
  }
}
