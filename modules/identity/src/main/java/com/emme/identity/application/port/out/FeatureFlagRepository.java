package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.FeatureFlag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Feature-flag persistence capability required by Identity application services. */
public interface FeatureFlagRepository {

  Optional<FeatureFlag> findTenantOverride(UUID tenantId, String code);

  List<FeatureFlag> findGlobalDefaults();

  List<FeatureFlag> findByTenantOrGlobal(UUID tenantId);

  FeatureFlag save(FeatureFlag featureFlag);
}
