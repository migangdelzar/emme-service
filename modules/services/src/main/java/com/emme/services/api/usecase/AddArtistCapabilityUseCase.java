package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistCapabilityDetails;
import java.util.UUID;

/** Assigns a service capability to an artist. */
public interface AddArtistCapabilityUseCase {

  ArtistCapabilityDetails add(UUID artistId, UUID serviceId, UUID tenantId);
}
