package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data mechanics for customer membership persistence. */
public interface SpringDataCustomerMembershipRepository
    extends JpaRepository<CustomerMembershipEntity, CustomerMembershipId> {

  List<CustomerMembershipEntity> findByCustomerId(UUID customerId);

  boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
}
