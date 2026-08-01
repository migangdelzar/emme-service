package com.emme.identity.application;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.Permission;
import com.emme.identity.adapter.out.persistence.entity.Role;
import com.emme.identity.adapter.out.persistence.entity.RolePermission;
import com.emme.identity.adapter.out.persistence.repository.PermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.RolePermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataMembershipRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.emme.identity.domain.model.MembershipStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IdentityService {

  private final SpringDataMembershipRepository membershipRepo;
  private final SpringDataRoleRepository roleRepo;
  private final PermissionRepository permissionRepo;
  private final RolePermissionRepository rolePermissionRepo;

  public IdentityService(
      SpringDataMembershipRepository mRepo,
      SpringDataRoleRepository rRepo,
      PermissionRepository pRepo,
      RolePermissionRepository rpRepo) {
    this.membershipRepo = mRepo;
    this.roleRepo = rRepo;
    this.permissionRepo = pRepo;
    this.rolePermissionRepo = rpRepo;
  }

  public MembershipEntity assignMembership(UUID tenantId, UUID roleId, String userReference) {
    Role role =
        roleRepo
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    MembershipEntity membership = new MembershipEntity(tenantId, role, userReference);
    return membershipRepo.save(membership);
  }

  public MembershipEntity revokeMembership(UUID membershipId) {
    MembershipEntity m =
        membershipRepo
            .findById(membershipId)
            .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
    m.getRole().getCode(); // initialize lazy proxy so controller serialization works
    m.revoke();
    return membershipRepo.save(m);
  }

  @Transactional(readOnly = true)
  public Set<String> getPermissionsForUser(String userReference, UUID tenantId) {
    List<MembershipEntity> memberships =
        membershipRepo.findByUserReferenceAndStatus(userReference, MembershipStatus.ACTIVE);
    if (memberships.isEmpty()) return Set.of();

    Set<UUID> roleIds =
        memberships.stream()
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(m -> m.getRole().getId())
            .collect(Collectors.toSet());

    List<RolePermission> rps = rolePermissionRepo.findByRoleIdIn(roleIds);
    Set<UUID> permissionIds =
        rps.stream().map(rp -> rp.getPermission().getId()).collect(Collectors.toSet());

    return permissionRepo.findAllById(permissionIds).stream()
        .map(Permission::getCode)
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public List<MembershipEntity> getCurrentUserMemberships(String userReference) {
    return membershipRepo.findByUserReferenceAndStatus(userReference, MembershipStatus.ACTIVE);
  }
}
