package com.emme.identity.application.service;

import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently establishes a customer's membership in a tenant. */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EnsureCustomerMembershipService implements EnsureCustomerMembershipUseCase {

  private final CustomerMembershipRepository repository;

  @Override
  public void ensure(EnsureCustomerMembershipCommand command) {
    ensureForCustomer(command.customerId(), command.tenantId());
  }

  public void ensureForCustomer(UUID customerId, UUID tenantId) {
    if (repository.existsByCustomerIdAndTenantId(customerId, tenantId)) {
      return;
    }

    CustomerMembership membership = new CustomerMembership(customerId, tenantId);
    if (repository.createIfAbsent(membership)) {
      log.info("Auto-created membership for customer {} in tenant {}", customerId, tenantId);
    }
  }
}
