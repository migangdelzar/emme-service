package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.util.Optional;
import java.util.UUID;

/** Retrieves a service-catalog entry. */
public interface GetCatalogServiceUseCase {

  Optional<Service> get(UUID id);
}
