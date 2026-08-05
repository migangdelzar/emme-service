package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistDetails;
import java.util.UUID;

/** Deactivates an artist. */
public interface DeactivateArtistUseCase {

  ArtistDetails deactivate(UUID id);
}
