package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.SuspendTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.SuspendTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SuspendTenantService implements SuspendTenantUseCase {
  private final TenantRepository repository;

  public SuspendTenantService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public TenantDetails suspend(SuspendTenantCommand command) {
    var tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.suspend();
    return TenantApplicationMapper.toResult(repository.save(tenant));
  }
}
