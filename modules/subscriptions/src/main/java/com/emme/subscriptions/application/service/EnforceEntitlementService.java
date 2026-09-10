package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.subscriptions.api.usecase.EnforceEntitlementUseCase;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnforceEntitlementService implements EnforceEntitlementUseCase {
  private final SubscriptionRepository repository;

  @Override
  public void enforce(EnforceEntitlementCommand command) {
    repository
        .find()
        .orElseThrow(
            () -> new IllegalArgumentException("No subscription for tenant: " + command.tenantId()))
        .enforce(command.entitlement());
  }
}
