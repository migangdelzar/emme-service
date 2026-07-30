package com.emme.studio.entity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {
  Optional<BusinessProfile> findByTenantId(UUID tenantId);
}
