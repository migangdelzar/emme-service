package com.emme.identity.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.MembershipStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeMembershipServiceTest {

  @Mock private MembershipRepository membershipRepository;

  @Test
  void loadsMembershipsThroughTheTenantScopedRepositoryPort() {
    UUID membershipId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Membership membership =
        Membership.rehydrate(
            membershipId,
            tenantId,
            UUID.randomUUID(),
            "tenant-owner",
            "user-1",
            MembershipStatus.ACTIVE,
            Instant.now(),
            Instant.now());
    when(membershipRepository.findByIdInTenant(membershipId, tenantId))
        .thenReturn(Optional.of(membership));
    when(membershipRepository.save(membership)).thenReturn(membership);

    new RevokeMembershipService(membershipRepository)
        .revoke(new RevokeMembershipCommand(membershipId, tenantId));

    verify(membershipRepository).findByIdInTenant(membershipId, tenantId);
  }
}
