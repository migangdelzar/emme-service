package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.ArtistCapability;
import java.util.List;
import java.util.UUID;

/** Lists capabilities assigned to an artist. */
public interface ListArtistCapabilitiesUseCase {

  List<ArtistCapability> list(UUID artistId);
}
