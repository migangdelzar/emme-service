package com.emme.identity.application.service;

import com.emme.identity.api.command.AssignMembershipCommand;
import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.AssignMembershipUseCase;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.RevokeMembershipUseCase;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates membership use cases through application-owned persistence ports. */
@Service
@Transactional
public class MembershipService
    implements AssignMembershipUseCase, GetCurrentUserMembershipsUseCase, RevokeMembershipUseCase {

  private final MembershipRepository membershipRepository;
  private final RoleRepository roleRepository;

  public MembershipService(
      MembershipRepository membershipRepository, RoleRepository roleRepository) {
    this.membershipRepository = membershipRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  public MembershipInfo assign(AssignMembershipCommand command) {
    return toMembershipInfo(assign(command.tenantId(), command.roleId(), command.userReference()));
  }

  @Override
  public MembershipInfo revoke(RevokeMembershipCommand command) {
    return toMembershipInfo(revoke(command.membershipId()));
  }

  @Override
  public List<MembershipInfo> getMemberships(GetCurrentUserMembershipsQuery query) {
    return membershipRepository.findActiveByUserReference(query.userReference()).stream()
        .map(MembershipService::toMembershipInfo)
        .toList();
  }

  public Membership assign(UUID tenantId, UUID roleId, String userReference) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    return membershipRepository.save(
        new Membership(tenantId, role.id(), role.code(), userReference));
  }

  public Membership revoke(UUID membershipId) {
    Membership membership =
        membershipRepository
            .findById(membershipId)
            .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
    membership.revoke();
    return membershipRepository.save(membership);
  }

  private static MembershipInfo toMembershipInfo(Membership membership) {
    return new MembershipInfo(
        membership.id(),
        membership.tenantId(),
        null,
        membership.roleCode(),
        membership.userReference(),
        membership.status().name(),
        membership.createdAt());
  }
}
