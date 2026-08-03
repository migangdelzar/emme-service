package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an artist by identifier. */
public interface GetArtistUseCase {

  Optional<ArtistDetails> get(UUID id);
}
