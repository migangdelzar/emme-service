package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistCapabilityDetails;
import java.util.UUID;

/** Assigns a service capability to an artist. */
public interface AddArtistCapabilityUseCase {

  ArtistCapabilityDetails add(UUID artistId, UUID serviceId, UUID tenantId);
}
