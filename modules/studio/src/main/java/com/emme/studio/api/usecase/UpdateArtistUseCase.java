package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistDetails;
import java.util.UUID;

/** Updates an artist. */
public interface UpdateArtistUseCase {

  ArtistDetails update(UUID id, String name);
}
