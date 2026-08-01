package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;

/** Persistence capability required to ensure customer access to a tenant. */
public interface CustomerMembershipRepository {

  boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);

  CustomerMembership save(CustomerMembership membership);
}
