package com.emme.identity.application.service;

import com.emme.identity.api.command.AssignMembershipCommand;
import com.emme.identity.api.exception.InvalidMembershipRoleException;
import com.emme.identity.api.result.MembershipInfo;
import com.emme.identity.api.usecase.AssignMembershipUseCase;
import com.emme.identity.application.mapper.MembershipApplicationMapper;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the AssignMembership use case. */
@Service
@Transactional
public class AssignMembershipService implements AssignMembershipUseCase {

  private final MembershipRepository membershipRepository;
  private final RoleRepository roleRepository;

  public AssignMembershipService(
      MembershipRepository membershipRepository, RoleRepository roleRepository) {
    this.membershipRepository = membershipRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  public MembershipInfo assign(AssignMembershipCommand command) {
    Role role =
        roleRepository
            .findById(command.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
    if (role.scope() != RoleScope.TENANT) {
      throw new InvalidMembershipRoleException(role.scope());
    }
    Membership membership =
        membershipRepository.save(
            new Membership(command.tenantId(), role.id(), role.code(), command.userReference()));
    return MembershipApplicationMapper.toInfo(membership);
  }
}
