package com.emme.identity.application.service;

import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.RevokeMembershipUseCase;
import com.emme.identity.application.mapper.MembershipApplicationMapper;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.domain.model.Membership;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the RevokeMembership use case. */
@Service
@Transactional
public class RevokeMembershipService implements RevokeMembershipUseCase {

  private final MembershipRepository membershipRepository;

  public RevokeMembershipService(MembershipRepository membershipRepository) {
    this.membershipRepository = membershipRepository;
  }

  @Override
  public MembershipInfo revoke(RevokeMembershipCommand command) {
    Membership membership =
        membershipRepository
            .findByIdInTenant(command.membershipId(), command.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
    membership.revoke();
    return MembershipApplicationMapper.toInfo(membershipRepository.save(membership));
  }
}
