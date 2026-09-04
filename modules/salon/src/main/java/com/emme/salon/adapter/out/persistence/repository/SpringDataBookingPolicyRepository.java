package com.emme.salon.adapter.out.persistence.repository;

import com.emme.salon.adapter.out.persistence.entity.BookingPolicyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBookingPolicyRepository
    extends JpaRepository<BookingPolicyEntity, UUID> {
  Optional<BookingPolicyEntity> findByTenantId(UUID tenantId);

  Optional<BookingPolicyEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
