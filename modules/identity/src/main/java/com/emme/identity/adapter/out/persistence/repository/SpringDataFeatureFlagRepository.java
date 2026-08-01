package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataFeatureFlagRepository extends JpaRepository<FeatureFlagEntity, UUID> {

  Optional<FeatureFlagEntity> findByTenantIdAndCode(UUID tenantId, String code);

  List<FeatureFlagEntity> findByTenantIdOrTenantIdIsNull(UUID tenantId);

  List<FeatureFlagEntity> findByTenantIdIsNull();
}
