package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.util.UUID;

/** Retires a service-catalog entry. */
public interface RetireCatalogServiceUseCase {

  Service retire(UUID id);
}
