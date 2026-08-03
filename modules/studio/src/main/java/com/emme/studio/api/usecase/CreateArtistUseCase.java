package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistDetails;
import java.util.UUID;

/** Creates an artist in a tenant. */
public interface CreateArtistUseCase {

  ArtistDetails create(UUID tenantId, String name);
}
