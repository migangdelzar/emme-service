package com.emme.identity.application.service;

import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently establishes a customer's membership in a tenant. */
@Service
@Transactional
public class EnsureCustomerMembershipService {

  private static final Logger log = LoggerFactory.getLogger(EnsureCustomerMembershipService.class);

  private final CustomerMembershipRepository repository;

  public EnsureCustomerMembershipService(CustomerMembershipRepository repository) {
    this.repository = repository;
  }

  public void ensureForCustomer(UUID customerId, UUID tenantId) {
    if (repository.existsByCustomerIdAndTenantId(customerId, tenantId)) {
      return;
    }

    repository.save(new CustomerMembership(customerId, tenantId));
    log.info("Auto-created membership for customer {} in tenant {}", customerId, tenantId);
  }
}
