package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.UpdateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.UpdateTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTenantService implements UpdateTenantUseCase {
  private final TenantRepository repository;

  public UpdateTenantService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public TenantInfo update(UpdateTenantCommand command) {
    Tenant tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.rename(command.name());
    return TenantApplicationMapper.toInfo(repository.save(tenant));
  }
}
