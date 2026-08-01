package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.PermissionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
  Optional<PermissionEntity> findByCode(String code);
}
