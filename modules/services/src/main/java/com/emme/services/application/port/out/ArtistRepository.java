package com.emme.services.application.port.out;

import com.emme.services.domain.model.Artist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Artist use cases. */
public interface ArtistRepository {

  Artist save(Artist artist);

  Optional<Artist> findById(UUID id);

  List<Artist> findAll();
}
