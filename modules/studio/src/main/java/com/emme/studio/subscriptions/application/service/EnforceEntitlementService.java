package com.emme.studio.subscriptions.application.service;

import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.studio.subscriptions.api.usecase.EnforceEntitlementUseCase;
import com.emme.studio.subscriptions.application.port.out.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnforceEntitlementService implements EnforceEntitlementUseCase {
  private final SubscriptionRepository repository;

  public EnforceEntitlementService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public void enforce(EnforceEntitlementCommand command) {
    repository
        .findByTenantId(command.tenantId())
        .orElseThrow(
            () -> new IllegalArgumentException("No subscription for tenant: " + command.tenantId()))
        .enforce(command.entitlement());
  }
}
