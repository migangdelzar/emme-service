package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.RolePermissionEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {
  List<RolePermissionEntity> findByRoleId(UUID roleId);

  List<RolePermissionEntity> findByRoleIdIn(Collection<UUID> roleIds);

  List<RolePermissionEntity> findByPermissionId(UUID permissionId);
}
