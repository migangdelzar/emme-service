package com.emme.studio.adapter.out.persistence.repository;

import com.emme.studio.adapter.out.persistence.entity.BusinessProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBusinessProfileRepository
    extends JpaRepository<BusinessProfileEntity, UUID> {
  Optional<BusinessProfileEntity> findByTenantId(UUID tenantId);
}
