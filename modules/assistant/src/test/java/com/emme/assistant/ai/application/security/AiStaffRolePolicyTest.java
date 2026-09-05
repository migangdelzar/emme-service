package com.emme.assistant.ai.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AiStaffRolePolicyTest {

  @Test
  void recognizesSupportedStaffRoleRepresentations() {
    assertThat(AiStaffRolePolicy.isStaff(Set.of("tenant_staff"))).isTrue();
    assertThat(AiStaffRolePolicy.isStaff(Set.of("ROLE_tenant_owner"))).isTrue();
    assertThat(AiStaffRolePolicy.isStaff(Set.of("ROLE_ADMIN"))).isTrue();
  }

  @Test
  void rejectsNonStaffRoles() {
    assertThat(AiStaffRolePolicy.isStaff(Set.of("tenant_client"))).isFalse();
    assertThat(AiStaffRolePolicy.isStaff(Set.of())).isFalse();
  }
}
