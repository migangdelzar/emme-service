package com.emme.identity.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureTenantMembershipServiceTest {

  @Mock private MembershipRepository membershipRepository;
  @Mock private RoleRepository roleRepository;

  @Test
  void createsTheTenantRoleAndMembershipThroughIdentityPorts() {
    UUID tenantId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    Role role = role("tenant_owner", roleId, true);
    when(roleRepository.findByCode("tenant_owner")).thenReturn(Optional.empty());
    when(roleRepository.save(any(Role.class))).thenReturn(role);
    when(membershipRepository.findByTenantIdAndUserReference(tenantId, "user-1"))
        .thenReturn(Optional.empty());
    Membership membership = new Membership(tenantId, roleId, role.code(), "user-1");
    when(membershipRepository.save(any(Membership.class))).thenReturn(membership);

    new EnsureTenantMembershipService(membershipRepository, roleRepository)
        .ensure(tenantId, "user-1", "tenant_owner");

    verify(roleRepository).save(any(Role.class));
    verify(membershipRepository).save(any(Membership.class));
  }

  @Test
  void doesNotDuplicateAnExistingMembership() {
    UUID tenantId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    Role role = role("tenant_owner", roleId, true);
    when(roleRepository.findByCode("tenant_owner")).thenReturn(Optional.of(role));
    when(membershipRepository.findByTenantIdAndUserReference(tenantId, "user-1"))
        .thenReturn(Optional.of(new Membership(tenantId, roleId, role.code(), "user-1")));

    new EnsureTenantMembershipService(membershipRepository, roleRepository)
        .ensure(tenantId, "user-1", "tenant_owner");

    verify(roleRepository, never()).save(any(Role.class));
    verify(membershipRepository, never()).save(any(Membership.class));
  }

  @Test
  void rejectsAPlatformRoleForTenantMembership() {
    UUID tenantId = UUID.randomUUID();
    Role role = role("platform_admin", UUID.randomUUID(), true, RoleScope.PLATFORM);
    when(roleRepository.findByCode("platform_admin")).thenReturn(Optional.of(role));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new EnsureTenantMembershipService(membershipRepository, roleRepository)
                    .ensure(tenantId, "user-1", "platform_admin"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role with scope PLATFORM cannot be assigned to a tenant membership");

    verify(membershipRepository, never()).save(any(Membership.class));
  }

  private static Role role(String code, UUID id, boolean active) {
    return role(code, id, active, RoleScope.TENANT);
  }

  private static Role role(String code, UUID id, boolean active, RoleScope scope) {
    Instant now = Instant.now();
    return Role.rehydrate(id, code, code, scope, active, now, now);
  }
}
