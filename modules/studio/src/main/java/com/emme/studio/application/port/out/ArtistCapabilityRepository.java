package com.emme.studio.application.port.out;

import com.emme.studio.domain.model.ArtistCapability;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability for artist/service relationships. */
public interface ArtistCapabilityRepository {

  ArtistCapability save(ArtistCapability capability);

  Optional<ArtistCapability> findById(UUID id);

  List<ArtistCapability> findByArtistId(UUID artistId);

  List<ArtistCapability> findByServiceIdAndActive(UUID serviceId);
}
