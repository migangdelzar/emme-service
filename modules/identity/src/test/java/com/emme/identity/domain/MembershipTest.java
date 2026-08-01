package com.emme.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.MembershipStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipTest {

  @Test
  void createsAnActiveMembershipWithItsRoleReference() {
    UUID tenantId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();

    Membership membership = new Membership(tenantId, roleId, "tenant_owner", "user-123");

    assertThat(membership.id()).isNull();
    assertThat(membership.tenantId()).isEqualTo(tenantId);
    assertThat(membership.roleId()).isEqualTo(roleId);
    assertThat(membership.roleCode()).isEqualTo("tenant_owner");
    assertThat(membership.userReference()).isEqualTo("user-123");
    assertThat(membership.status()).isEqualTo(MembershipStatus.ACTIVE);
  }

  @Test
  void preservesMembershipLifecycleOperations() {
    Membership membership =
        new Membership(UUID.randomUUID(), UUID.randomUUID(), "tenant_owner", "user-123");

    membership.suspend();
    assertThat(membership.status()).isEqualTo(MembershipStatus.SUSPENDED);

    membership.reactivate();
    assertThat(membership.status()).isEqualTo(MembershipStatus.ACTIVE);

    membership.revoke();
    assertThat(membership.status()).isEqualTo(MembershipStatus.REVOKED);
  }
}
