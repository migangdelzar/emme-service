package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Artist;
import java.util.UUID;

/** Creates an artist in a tenant. */
public interface CreateArtistUseCase {

  Artist create(UUID tenantId, String name);
}
