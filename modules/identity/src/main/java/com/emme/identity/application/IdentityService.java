package com.emme.identity.application;

import com.emme.identity.entity.Membership;
import com.emme.identity.entity.MembershipRepository;
import com.emme.identity.entity.MembershipStatus;
import com.emme.identity.entity.Permission;
import com.emme.identity.entity.PermissionRepository;
import com.emme.identity.entity.Role;
import com.emme.identity.entity.RolePermission;
import com.emme.identity.entity.RolePermissionRepository;
import com.emme.identity.entity.RoleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IdentityService {

  private final MembershipRepository membershipRepo;
  private final RoleRepository roleRepo;
  private final PermissionRepository permissionRepo;
  private final RolePermissionRepository rolePermissionRepo;

  public IdentityService(
      MembershipRepository mRepo,
      RoleRepository rRepo,
      PermissionRepository pRepo,
      RolePermissionRepository rpRepo) {
    this.membershipRepo = mRepo;
    this.roleRepo = rRepo;
    this.permissionRepo = pRepo;
    this.rolePermissionRepo = rpRepo;
  }

  public Membership assignMembership(UUID tenantId, UUID roleId, String userReference) {
    Role role =
        roleRepo
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    Membership membership = new Membership(tenantId, role, userReference);
    return membershipRepo.save(membership);
  }

  public Membership revokeMembership(UUID membershipId) {
    Membership m =
        membershipRepo
            .findById(membershipId)
            .orElseThrow(() -> new IllegalArgumentException("Membership not found"));
    m.getRole().getCode(); // initialize lazy proxy so controller serialization works
    m.revoke();
    return membershipRepo.save(m);
  }

  @Transactional(readOnly = true)
  public Set<String> getPermissionsForUser(String userReference, UUID tenantId) {
    List<Membership> memberships =
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
  public List<Membership> getCurrentUserMemberships(String userReference) {
    return membershipRepo.findByUserReferenceAndStatus(userReference, MembershipStatus.ACTIVE);
  }
}
