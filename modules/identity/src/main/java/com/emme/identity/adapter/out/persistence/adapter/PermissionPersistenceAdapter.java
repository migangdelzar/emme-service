package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.entity.PermissionEntity;
import com.emme.identity.adapter.out.persistence.entity.RolePermissionEntity;
import com.emme.identity.adapter.out.persistence.repository.PermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.RolePermissionRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataMembershipRepository;
import com.emme.identity.application.port.out.PermissionPort;
import com.emme.identity.domain.model.MembershipStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolves tenant-scoped permission codes from Identity persistence models. */
@Component
public class PermissionPersistenceAdapter implements PermissionPort {

  private final SpringDataMembershipRepository membershipRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PermissionRepository permissionRepository;

  public PermissionPersistenceAdapter(
      SpringDataMembershipRepository membershipRepository,
      RolePermissionRepository rolePermissionRepository,
      PermissionRepository permissionRepository) {
    this.membershipRepository = membershipRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.permissionRepository = permissionRepository;
  }

  @Override
  public Set<String> findPermissionCodesForUserInTenant(String userReference, UUID tenantId) {
    List<UUID> roleIds =
        membershipRepository
            .findByUserReferenceAndStatus(userReference, MembershipStatus.ACTIVE)
            .stream()
            .filter(membership -> membership.getTenantId().equals(tenantId))
            .map(membership -> membership.getRole().getId())
            .toList();
    if (roleIds.isEmpty()) {
      return Set.of();
    }

    Set<UUID> permissionIds =
        rolePermissionRepository.findByRoleIdIn(roleIds).stream()
            .map(RolePermissionEntity::getPermission)
            .map(PermissionEntity::getId)
            .collect(Collectors.toSet());
    return permissionRepository.findAllById(permissionIds).stream()
        .map(PermissionEntity::getCode)
        .collect(Collectors.toSet());
  }
}
