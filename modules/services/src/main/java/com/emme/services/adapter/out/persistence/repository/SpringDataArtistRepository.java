package com.emme.services.adapter.out.persistence.repository;

import com.emme.services.adapter.out.persistence.entity.ArtistEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataArtistRepository extends JpaRepository<ArtistEntity, UUID> {
  List<ArtistEntity> findByTenantId(UUID tenantId);

  Optional<ArtistEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
