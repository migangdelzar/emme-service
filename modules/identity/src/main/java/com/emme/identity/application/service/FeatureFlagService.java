package com.emme.identity.application.service;

import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.api.result.EffectiveFeatureFlags;
import com.emme.identity.api.result.FeatureFlagInfo;
import com.emme.identity.api.usecase.GetEffectiveFeatureFlagsUseCase;
import com.emme.identity.api.usecase.SetPlatformFeatureFlagUseCase;
import com.emme.identity.api.usecase.SetTenantFeatureFlagOverrideUseCase;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.identity.domain.model.FeatureFlag;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.studio.subscriptions.api.PlanType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves and manages feature flags through application-owned capabilities. */
@Service("featureFlagService")
@Transactional
public class FeatureFlagService
    implements GetEffectiveFeatureFlagsUseCase,
        SetPlatformFeatureFlagUseCase,
        SetTenantFeatureFlagOverrideUseCase {

  private final FeatureFlagRepository repository;
  private final SubscriptionPlanPort subscriptionPlanPort;

  public FeatureFlagService(
      FeatureFlagRepository repository, SubscriptionPlanPort subscriptionPlanPort) {
    this.repository = repository;
    this.subscriptionPlanPort = subscriptionPlanPort;
  }

  /** SpEL-accessible tenant-aware feature check. */
  public boolean isEnabled(String code) {
    UUID tenantId = TenantContextHolder.currentTenantOptional().orElse(null);

    if (tenantId != null) {
      Optional<FeatureFlag> tenantFlag = repository.findTenantOverride(tenantId, code);
      if (tenantFlag.isPresent()) {
        return tenantFlag.get().isEnabled();
      }
    }

    Optional<FeatureFlag> globalFlag =
        repository.findGlobalDefaults().stream()
            .filter(flag -> flag.code().equals(code))
            .findFirst();
    if (globalFlag.isEmpty()) {
      return false;
    }

    FeatureFlag flag = globalFlag.get();
    if (!flag.isEnabled()) {
      return false;
    }
    if (flag.planRequired() != null && tenantId != null) {
      return subscriptionPlanPort
          .findPlanForTenant(tenantId)
          .map(plan -> plan.ordinal() >= flag.planRequired().ordinal())
          .orElse(false);
    }
    return true;
  }

  /** Builds the effective global-default and tenant-override map. */
  @Transactional(readOnly = true)
  public Map<String, Boolean> getEffective(UUID tenantId) {
    Map<String, Boolean> effective = new HashMap<>();
    for (FeatureFlag global : repository.findGlobalDefaults()) {
      effective.put(global.code(), global.isEnabled());
    }
    List<FeatureFlag> allFlags = repository.findByTenantOrGlobal(tenantId);
    for (FeatureFlag flag : allFlags) {
      if (flag.tenantId() != null) {
        effective.put(flag.code(), flag.isEnabled());
      }
    }
    return effective;
  }

  @Override
  public EffectiveFeatureFlags get(GetEffectiveFeatureFlagsQuery query) {
    return new EffectiveFeatureFlags(getEffective(query.tenantId()));
  }

  @Override
  public FeatureFlagInfo set(SetTenantFeatureFlagOverrideCommand command) {
    return toFeatureFlagInfo(setOverride(command.tenantId(), command.code(), command.enabled()));
  }

  @Override
  public FeatureFlagInfo set(SetPlatformFeatureFlagCommand command) {
    return toFeatureFlagInfo(
        platformSet(command.code(), command.enabled(), command.planRequired()));
  }

  public FeatureFlag setOverride(UUID tenantId, String code, boolean enabled) {
    Optional<FeatureFlag> existing = repository.findTenantOverride(tenantId, code);
    if (existing.isPresent()) {
      FeatureFlag flag = existing.get();
      flag.changeEnabled(enabled);
      return repository.save(flag);
    }
    return repository.save(new FeatureFlag(tenantId, code, enabled, null, "Tenant override"));
  }

  public FeatureFlag platformSet(String code, boolean enabled, PlanType planRequired) {
    Optional<FeatureFlag> existing =
        repository.findGlobalDefaults().stream()
            .filter(flag -> flag.code().equals(code))
            .findFirst();
    if (existing.isPresent()) {
      FeatureFlag flag = existing.get();
      flag.changeEnabled(enabled);
      return repository.save(flag);
    }
    return repository.save(new FeatureFlag(null, code, enabled, planRequired, null));
  }

  private static FeatureFlagInfo toFeatureFlagInfo(FeatureFlag featureFlag) {
    return new FeatureFlagInfo(
        featureFlag.id(),
        featureFlag.code(),
        featureFlag.isEnabled(),
        featureFlag.planRequired(),
        featureFlag.description());
  }
}
