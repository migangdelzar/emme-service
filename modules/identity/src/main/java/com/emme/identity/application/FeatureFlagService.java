package com.emme.identity.application;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlag;
import com.emme.identity.adapter.out.persistence.repository.FeatureFlagRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.application.SubscriptionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("featureFlagService")
@Transactional
public class FeatureFlagService {

  private final FeatureFlagRepository repo;
  private final SubscriptionService subscriptionService;

  public FeatureFlagService(FeatureFlagRepository repo, SubscriptionService subscriptionService) {
    this.repo = repo;
    this.subscriptionService = subscriptionService;
  }

  /**
   * SpEL-accessible method: checks whether a feature flag is enabled for the current tenant
   * context.
   *
   * <p>Resolution order: tenant override → global default → plan requirement.
   */
  public boolean isEnabled(String code) {
    UUID tenantId = TenantContextHolder.currentTenantOptional().orElse(null);

    // 1. Tenant-specific override
    if (tenantId != null) {
      Optional<FeatureFlag> tenantFlag = repo.findByTenantIdAndCode(tenantId, code);
      if (tenantFlag.isPresent()) {
        return tenantFlag.get().isEnabled();
      }
    }

    // 2. Global default
    Optional<FeatureFlag> globalFlag =
        repo.findByTenantIdIsNull().stream().filter(f -> f.getCode().equals(code)).findFirst();

    if (globalFlag.isPresent()) {
      FeatureFlag flag = globalFlag.get();
      if (!flag.isEnabled()) {
        return false;
      }

      // 3. Plan requirement gate
      if (flag.getPlanRequired() != null && tenantId != null) {
        return subscriptionService
            .getPlanForTenant(tenantId)
            .map(plan -> plan.ordinal() >= flag.getPlanRequired().ordinal())
            .orElse(false);
      }

      return true;
    }

    return false;
  }

  /**
   * Builds the effective flag map for a tenant by merging global defaults with tenant-specific
   * overrides.
   */
  @Transactional(readOnly = true)
  public Map<String, Boolean> getEffective(UUID tenantId) {
    Map<String, Boolean> effective = new HashMap<>();

    // Load globals first
    List<FeatureFlag> globals = repo.findByTenantIdIsNull();
    for (FeatureFlag global : globals) {
      effective.put(global.getCode(), global.isEnabled());
    }

    // Overlay tenant-specific overrides
    List<FeatureFlag> allFlags = repo.findByTenantIdOrTenantIdIsNull(tenantId);
    for (FeatureFlag flag : allFlags) {
      if (flag.getTenantId() != null) {
        effective.put(flag.getCode(), flag.isEnabled());
      }
    }

    return effective;
  }

  /** Creates or updates a tenant-specific feature flag override. */
  public FeatureFlag setOverride(UUID tenantId, String code, boolean enabled) {
    Optional<FeatureFlag> existing = repo.findByTenantIdAndCode(tenantId, code);
    if (existing.isPresent()) {
      FeatureFlag flag = existing.get();
      flag.setEnabled(enabled);
      return repo.save(flag);
    }
    FeatureFlag flag = new FeatureFlag(tenantId, code, enabled, null, "Tenant override");
    return repo.save(flag);
  }

  /** Creates or updates a global (platform-wide) feature flag default. */
  public FeatureFlag platformSet(String code, boolean enabled, PlanType planRequired) {
    Optional<FeatureFlag> existing =
        repo.findByTenantIdIsNull().stream().filter(f -> f.getCode().equals(code)).findFirst();
    if (existing.isPresent()) {
      FeatureFlag flag = existing.get();
      flag.setEnabled(enabled);
      return repo.save(flag);
    }
    FeatureFlag flag = new FeatureFlag(null, code, enabled, planRequired, null);
    return repo.save(flag);
  }
}
