package com.emme.services.adapter.out.persistence.repository;

import com.emme.services.adapter.out.persistence.entity.ArtistCapabilityEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataArtistCapabilityRepository
    extends JpaRepository<ArtistCapabilityEntity, UUID> {
  List<ArtistCapabilityEntity> findByArtistId(UUID artistId);

  Optional<ArtistCapabilityEntity> findByArtistIdAndServiceId(UUID artistId, UUID serviceId);

  List<ArtistCapabilityEntity> findByServiceIdAndActiveTrue(UUID serviceId);
}
