package com.emme.identity.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerMembershipRepository
    extends JpaRepository<CustomerMembership, CustomerMembership.MembershipId> {
  List<CustomerMembership> findByCustomerId(UUID customerId);

  boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
