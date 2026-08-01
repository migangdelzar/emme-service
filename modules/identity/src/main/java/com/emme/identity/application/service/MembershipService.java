package com.emme.identity.application.service;

import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleReference;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates membership use cases through application-owned persistence ports. */
@Service
@Transactional
public class MembershipService {

  private final MembershipRepository membershipRepository;
  private final RoleRepository roleRepository;

  public MembershipService(
      MembershipRepository membershipRepository, RoleRepository roleRepository) {
    this.membershipRepository = membershipRepository;
    this.roleRepository = roleRepository;
  }

  public Membership assign(UUID tenantId, UUID roleId, String userReference) {
    RoleReference role =
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

  @Transactional(readOnly = true)
  public List<Membership> findCurrentUserMemberships(String userReference) {
    return membershipRepository.findActiveByUserReference(userReference);
  }

  @Transactional(readOnly = true)
  public List<Membership> findUserMemberships(String userReference) {
    return membershipRepository.findByUserReference(userReference);
  }
}
