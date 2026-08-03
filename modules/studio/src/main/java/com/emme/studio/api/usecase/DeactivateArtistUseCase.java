package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistDetails;
import java.util.UUID;

/** Deactivates an artist. */
public interface DeactivateArtistUseCase {

  ArtistDetails deactivate(UUID id);
}
