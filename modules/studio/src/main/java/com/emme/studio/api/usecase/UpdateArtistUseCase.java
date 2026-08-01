package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Artist;
import java.util.UUID;

/** Updates an artist. */
public interface UpdateArtistUseCase {

  Artist update(UUID id, String name);
}
