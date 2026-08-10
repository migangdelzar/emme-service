package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistDetails;
import java.util.UUID;

/** Creates an artist in a tenant. */
public interface CreateArtistUseCase {

  ArtistDetails create(UUID tenantId, String name);
}
