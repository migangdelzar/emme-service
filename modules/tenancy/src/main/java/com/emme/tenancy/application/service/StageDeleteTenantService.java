package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.StageDeleteTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.StageDeleteTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StageDeleteTenantService implements StageDeleteTenantUseCase {
  private final TenantRepository repository;

  public StageDeleteTenantService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public TenantInfo stageDelete(StageDeleteTenantCommand command) {
    var tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.markDeleted();
    return TenantApplicationMapper.toInfo(repository.save(tenant));
  }
}
