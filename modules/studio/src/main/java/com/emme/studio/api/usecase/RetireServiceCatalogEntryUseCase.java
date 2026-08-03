package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.util.UUID;

/** Retires a service-catalog entry. */
public interface RetireServiceCatalogEntryUseCase {

  Service retire(UUID id);
}
