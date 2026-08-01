package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.RolePermission;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
  List<RolePermission> findByRoleId(UUID roleId);

  List<RolePermission> findByRoleIdIn(Collection<UUID> roleIds);

  List<RolePermission> findByPermissionId(UUID permissionId);
}
