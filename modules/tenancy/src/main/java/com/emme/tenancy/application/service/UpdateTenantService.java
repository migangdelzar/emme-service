package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.UpdateTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.UpdateTenantUseCase;
import com.emme.tenancy.application.mapper.TenantApplicationMapper;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateTenantService implements UpdateTenantUseCase {
  private final TenantRepository repository;

  @Override
  public TenantDetails update(UpdateTenantCommand command) {
    Tenant tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.rename(command.name());
    return TenantApplicationMapper.toResult(repository.save(tenant));
  }
}
