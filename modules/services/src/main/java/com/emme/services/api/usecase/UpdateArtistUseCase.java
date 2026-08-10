package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistDetails;
import java.util.UUID;

/** Updates an artist. */
public interface UpdateArtistUseCase {

  ArtistDetails update(UUID id, String name);
}
