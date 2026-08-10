package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistCapabilityDetails;
import java.util.UUID;

/** Removes an artist capability. */
public interface RemoveArtistCapabilityUseCase {

  ArtistCapabilityDetails remove(UUID capabilityId);
}
