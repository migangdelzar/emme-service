package com.emme.identity.application.authorization;

import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.identity.domain.model.FeatureFlag;
import com.emme.kernel.context.TenantContextHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Evaluates feature flags for authorization and application policy decisions. */
@Service("featureFlagService")
@Transactional(readOnly = true)
public class FeatureFlagEvaluator {

  private final FeatureFlagRepository repository;
  private final SubscriptionPlanPort subscriptionPlanPort;

  public FeatureFlagEvaluator(
      FeatureFlagRepository repository, SubscriptionPlanPort subscriptionPlanPort) {
    this.repository = repository;
    this.subscriptionPlanPort = subscriptionPlanPort;
  }

  /** Evaluates a feature flag using the current tenant context. */
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
}
