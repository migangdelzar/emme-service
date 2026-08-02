package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.identity.api.exception.InvalidMembershipRoleException;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignMembershipServiceTest {

  @Mock private MembershipRepository membershipRepository;
  @Mock private RoleRepository roleRepository;

  @Test
  void rejectsPlatformScopedRolesFromTenantMemberships() {
    UUID roleId = UUID.randomUUID();
    when(roleRepository.findById(roleId))
        .thenReturn(
            Optional.of(
                Role.rehydrate(
                    roleId,
                    "platform_admin",
                    "Platform admin",
                    RoleScope.PLATFORM,
                    true,
                    java.time.Instant.now(),
                    java.time.Instant.now())));

    assertThatThrownBy(
            () ->
                new AssignMembershipService(membershipRepository, roleRepository)
                    .assign(
                        new com.emme.identity.api.command.AssignMembershipCommand(
                            UUID.randomUUID(), roleId, "user-1")))
        .isInstanceOf(IllegalArgumentException.class)
        .isInstanceOf(InvalidMembershipRoleException.class)
        .hasMessage("Role with scope PLATFORM cannot be assigned to a tenant membership");

    verify(membershipRepository, never()).save(org.mockito.ArgumentMatchers.any(Membership.class));
  }
}
