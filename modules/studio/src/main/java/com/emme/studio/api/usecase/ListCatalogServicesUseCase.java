package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.Service;
import com.emme.studio.domain.model.ServiceStatus;
import java.util.List;
import java.util.UUID;

/** Lists service-catalog entries for a tenant. */
public interface ListCatalogServicesUseCase {

  List<Service> list(UUID tenantId, ServiceStatus status);
}
