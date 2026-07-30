package com.emme.studio.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistCapabilityRepository extends JpaRepository<ArtistCapability, UUID> {
  List<ArtistCapability> findByTenantId(UUID tenantId);

  List<ArtistCapability> findByArtistId(UUID artistId);

  Optional<ArtistCapability> findByArtistIdAndServiceId(UUID artistId, UUID serviceId);

  List<ArtistCapability> findByServiceIdAndActiveTrue(UUID serviceId);
}
