package com.emme.studio.application.port.out;

import com.emme.studio.domain.model.Artist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Artist use cases. */
public interface ArtistRepository {

  Artist save(Artist artist);

  Optional<Artist> findById(UUID id);

  List<Artist> findByTenantId(UUID tenantId);
}
