package com.emme.tenancy.application.service;

import com.emme.tenancy.api.query.GetTenantProvisioningStatusQuery;
import com.emme.tenancy.api.result.TenantProvisioningStatus;
import com.emme.tenancy.api.type.TenantProvisioningState;
import com.emme.tenancy.api.usecase.GetTenantProvisioningStatusUseCase;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetTenantProvisioningStatusService implements GetTenantProvisioningStatusUseCase {
  private final TenantProvisioningRepository repository;

  public GetTenantProvisioningStatusService(TenantProvisioningRepository repository) {
    this.repository = repository;
  }

  @Override
  public TenantProvisioningStatus get(GetTenantProvisioningStatusQuery query) {
    var status = repository.findStatus(query.tenantId());
    return new TenantProvisioningStatus(
        TenantProvisioningState.valueOf(status.status().name()),
        status.schemaName(),
        status.lastMigratedAt(),
        status.error());
  }
}
