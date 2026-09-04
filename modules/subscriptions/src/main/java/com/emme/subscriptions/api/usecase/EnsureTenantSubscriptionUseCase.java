package com.emme.subscriptions.api.usecase;

import java.util.UUID;

/** Ensures the default subscription exists for a newly activated tenant. */
public interface EnsureTenantSubscriptionUseCase {

  void ensure(UUID tenantId);
}
