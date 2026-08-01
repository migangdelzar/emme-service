package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import java.math.BigDecimal;
import java.util.UUID;

/** Creates a service-catalog entry. */
public interface CreateCatalogServiceUseCase {

  Service create(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice);
}
