package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.ArtistCapability;
import java.util.UUID;

/** Assigns a service capability to an artist. */
public interface AddArtistCapabilityUseCase {

  ArtistCapability add(UUID artistId, UUID serviceId, UUID tenantId);
}
