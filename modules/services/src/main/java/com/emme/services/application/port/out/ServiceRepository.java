package com.emme.services.application.port.out;

import com.emme.services.domain.model.Service;
import com.emme.services.domain.model.ServiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Service Catalog use cases. */
public interface ServiceRepository {

  Service save(Service service);

  Optional<Service> findById(UUID id);

  List<Service> findByTenantIdAndStatus(UUID tenantId, ServiceStatus status);
}
