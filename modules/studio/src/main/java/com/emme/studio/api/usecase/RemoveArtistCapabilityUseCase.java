package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.ArtistCapability;
import java.util.UUID;

/** Removes an artist capability. */
public interface RemoveArtistCapabilityUseCase {

  ArtistCapability remove(UUID capabilityId);
}
