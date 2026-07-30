package com.emme.identity.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CustomerMembershipRepository
        extends JpaRepository<CustomerMembership, CustomerMembership.MembershipId> {
    List<CustomerMembership> findByCustomerId(UUID customerId);
    boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
