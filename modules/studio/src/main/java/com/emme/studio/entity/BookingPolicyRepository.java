package com.emme.studio.entity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, UUID> {
  Optional<BookingPolicy> findByTenantId(UUID tenantId);
}
