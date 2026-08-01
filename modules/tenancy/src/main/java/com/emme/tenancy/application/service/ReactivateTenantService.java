package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.ReactivateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.ReactivateTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReactivateTenantService implements ReactivateTenantUseCase {
  private final TenantRepository repository;

  public ReactivateTenantService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public TenantInfo reactivate(ReactivateTenantCommand command) {
    var tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.reactivate();
    return TenantApplicationMapper.toInfo(repository.save(tenant));
  }
}
