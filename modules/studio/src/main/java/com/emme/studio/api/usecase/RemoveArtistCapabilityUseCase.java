package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistCapabilityDetails;
import java.util.UUID;

/** Removes an artist capability. */
public interface RemoveArtistCapabilityUseCase {

  ArtistCapabilityDetails remove(UUID capabilityId);
}
