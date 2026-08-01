package com.emme.identity.application.service;

import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.domain.model.FeatureFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class FeatureFlagTestRepository implements FeatureFlagRepository {

  final List<FeatureFlag> flags = new ArrayList<>();

  void addGlobal(String code, boolean enabled) {
    flags.add(new FeatureFlag(null, code, enabled, null, "Global default"));
  }

  void addTenantOverride(UUID tenantId, String code, boolean enabled) {
    flags.add(new FeatureFlag(tenantId, code, enabled, null, "Tenant override"));
  }

  @Override
  public Optional<FeatureFlag> findTenantOverride(UUID tenantId, String code) {
    return flags.stream()
        .filter(flag -> tenantId.equals(flag.tenantId()) && code.equals(flag.code()))
        .findFirst();
  }

  @Override
  public List<FeatureFlag> findGlobalDefaults() {
    return flags.stream().filter(flag -> flag.tenantId() == null).toList();
  }

  @Override
  public List<FeatureFlag> findByTenantOrGlobal(UUID tenantId) {
    return flags.stream()
        .filter(flag -> flag.tenantId() == null || tenantId.equals(flag.tenantId()))
        .toList();
  }

  @Override
  public FeatureFlag save(FeatureFlag flag) {
    flags.removeIf(
        current ->
            current.tenantId() == null
                ? flag.tenantId() == null && current.code().equals(flag.code())
                : current.tenantId().equals(flag.tenantId()) && current.code().equals(flag.code()));
    flags.add(flag);
    return flag;
  }
}
