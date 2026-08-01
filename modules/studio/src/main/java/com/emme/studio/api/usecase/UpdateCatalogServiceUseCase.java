package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.math.BigDecimal;
import java.util.UUID;

/** Updates a service-catalog entry. */
public interface UpdateCatalogServiceUseCase {

  Service update(
      UUID id,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice);
}
