package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Artist;
import java.util.List;
import java.util.UUID;

/** Lists artists belonging to a tenant. */
public interface ListTenantArtistsUseCase {

  List<Artist> list(UUID tenantId);
}
