package com.emme.studio.api.usecase;

import com.emme.studio.api.result.ArtistDetails;
import java.util.List;
import java.util.UUID;

/** Lists artists belonging to a tenant. */
public interface ListTenantArtistsUseCase {

  List<ArtistDetails> list(UUID tenantId);
}
