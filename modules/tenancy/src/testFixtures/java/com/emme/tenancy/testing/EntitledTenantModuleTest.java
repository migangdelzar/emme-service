package com.emme.tenancy.testing;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.adapter.out.persistence.repository.SpringDataFeatureFlagRepository;
import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.tenancy.api.result.TenantDetails;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/** Tenant module fixture for tests that require an active subscription and feature flags. */
public abstract class EntitledTenantModuleTest extends BaseTenantModuleTest {

  @Autowired protected SpringDataSubscriptionRepository subscriptionRepo;
  @Autowired protected SpringDataFeatureFlagRepository featureFlagRepo;

  /** Create a tenant with an ENTERPRISE subscription and the global feature flags used by tests. */
  protected UUID fullSetup() {
    TenantDetails tenant = createTenant("test-" + System.nanoTime(), "Test Salon");
    UUID tid = tenant.id();
    tenantId = tid;

    if (subscriptionRepo.findAll().stream()
        .noneMatch(subscription -> tid.equals(subscription.getTenantId()))) {
      subscriptionRepo.save(
          new SubscriptionEntity(
              tid, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
    }

    String[] flags = {
      "ai_chat",
      "analytics_export",
      "calendar_sync",
      "whatsapp_booking",
      "google_workspace",
      "google_sheets_export",
      "client_google_sync"
    };
    for (String code : flags) {
      if (featureFlagRepo.findByTenantIdIsNull().stream()
          .noneMatch(featureFlag -> featureFlag.getCode().equals(code))) {
        featureFlagRepo.save(new FeatureFlagEntity(null, code, true, null, "global default"));
      }
    }
    return tid;
  }
}
