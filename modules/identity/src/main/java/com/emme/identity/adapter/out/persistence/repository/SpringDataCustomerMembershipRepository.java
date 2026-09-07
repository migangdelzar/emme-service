package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.CustomerMembershipId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Spring Data mechanics for customer membership persistence. */
public interface SpringDataCustomerMembershipRepository
    extends JpaRepository<CustomerMembershipEntity, CustomerMembershipId> {

  List<CustomerMembershipEntity> findByCustomerId(UUID customerId);

  boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId);

  @Modifying
  @Query(
      value =
          "INSERT INTO emme_core.customer_membership "
              + "(customer_id, tenant_id, created_at) VALUES (?1, ?2, ?3) "
              + "ON CONFLICT (customer_id, tenant_id) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(UUID customerId, UUID tenantId, java.time.Instant createdAt);
}
