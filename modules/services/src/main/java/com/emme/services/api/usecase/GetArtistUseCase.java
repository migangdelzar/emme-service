package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an artist by identifier. */
public interface GetArtistUseCase {

  Optional<ArtistDetails> get(UUID id);
}
