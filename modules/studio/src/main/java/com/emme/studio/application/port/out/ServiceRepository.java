package com.emme.studio.application.port.out;

import com.emme.studio.domain.model.Service;
import com.emme.studio.domain.model.ServiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Service Catalog use cases. */
public interface ServiceRepository {

  Service save(Service service);

  Optional<Service> findById(UUID id);

  List<Service> findByTenantIdAndStatus(UUID tenantId, ServiceStatus status);
}
