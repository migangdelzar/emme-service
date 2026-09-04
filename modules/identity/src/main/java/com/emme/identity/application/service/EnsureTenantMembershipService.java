package com.emme.identity.application.service;

import com.emme.identity.api.exception.InvalidMembershipRoleException;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.application.port.out.RoleRepository;
import com.emme.identity.domain.model.Membership;
import com.emme.identity.domain.model.Role;
import com.emme.identity.domain.model.RoleScope;
import com.emme.tenancy.api.usecase.EnsureTenantMembershipUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ensures identity memberships during tenant realm provisioning. */
@Service
@Transactional
public class EnsureTenantMembershipService implements EnsureTenantMembershipUseCase {

  private final MembershipRepository membershipRepository;
  private final RoleRepository roleRepository;

  public EnsureTenantMembershipService(
      MembershipRepository membershipRepository, RoleRepository roleRepository) {
    this.membershipRepository = membershipRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  public void ensure(UUID tenantId, String userReference, String roleCode) {
    Role role = ensureTenantRole(roleCode);
    if (membershipRepository.findByTenantIdAndUserReference(tenantId, userReference).isEmpty()) {
      membershipRepository.save(new Membership(tenantId, role.id(), role.code(), userReference));
    }
  }

  private Role ensureTenantRole(String roleCode) {
    Role role =
        roleRepository
            .findByCode(roleCode)
            .orElseGet(
                () ->
                    roleRepository.save(
                        new Role(roleCode, readableName(roleCode), RoleScope.TENANT)));
    if (role.scope() != RoleScope.TENANT) {
      throw new InvalidMembershipRoleException(role.scope().name());
    }
    if (!role.isActive()) {
      role.activate();
      return roleRepository.save(role);
    }
    return role;
  }

  private static String readableName(String roleCode) {
    return roleCode.replace('_', ' ');
  }
}
