package com.emme.tenancy.application.service;

import com.emme.tenancy.api.command.UpdateTenantIdentityRealmCommand;
import com.emme.tenancy.api.usecase.UpdateTenantIdentityRealmUseCase;
import com.emme.tenancy.application.port.out.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateTenantIdentityRealmService implements UpdateTenantIdentityRealmUseCase {
  private final TenantRepository repository;

  public UpdateTenantIdentityRealmService(TenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public void update(UpdateTenantIdentityRealmCommand command) {
    var tenant = TenantServiceSupport.require(repository, command.tenantId());
    tenant.changeIdentityRealm(command.identityRealm());
    repository.save(tenant);
  }
}
