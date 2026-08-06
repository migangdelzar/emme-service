package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.RequestTenantProvisioningCommand;
import com.emme.tenancy.api.usecase.RequestTenantProvisioningUseCase;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RequestTenantProvisioningService implements RequestTenantProvisioningUseCase {
  private final TenantProvisioningRepository repository;

  public RequestTenantProvisioningService(TenantProvisioningRepository repository) {
    this.repository = repository;
  }

  @Override
  public UUID request(RequestTenantProvisioningCommand command) {
    var tenantId = UUID.randomUUID();
    String schemaName =
        command.slug().replace('-', '_').replaceAll("[^a-z0-9_]", "_").toLowerCase();
    return repository.requestProvisioning(tenantId, command.slug(), schemaName);
  }
}
