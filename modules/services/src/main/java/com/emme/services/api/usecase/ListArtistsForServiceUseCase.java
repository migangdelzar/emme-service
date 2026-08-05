package com.emme.services.api.usecase;

import com.emme.services.api.result.ArtistDetails;
import java.util.List;
import java.util.UUID;

/** Lists artists that can perform a service for a tenant. */
public interface ListArtistsForServiceUseCase {

  List<ArtistDetails> list(UUID tenantId, UUID serviceId);
}
