package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Artist;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an artist by identifier. */
public interface GetArtistUseCase {

  Optional<Artist> get(UUID id);
}
