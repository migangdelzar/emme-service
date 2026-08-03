package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Artist;
import java.util.UUID;

/** Deactivates an artist. */
public interface DeactivateArtistUseCase {

  Artist deactivate(UUID id);
}
