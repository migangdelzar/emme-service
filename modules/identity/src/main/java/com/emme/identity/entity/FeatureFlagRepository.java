package com.emme.identity.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

  Optional<FeatureFlag> findByTenantIdAndCode(UUID tenantId, String code);

  List<FeatureFlag> findByTenantIdOrTenantIdIsNull(UUID tenantId);

  List<FeatureFlag> findByTenantIdIsNull();
}
