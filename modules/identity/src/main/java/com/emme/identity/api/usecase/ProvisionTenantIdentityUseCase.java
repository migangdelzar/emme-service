package com.emme.identity.api.usecase;

import com.emme.identity.api.command.ProvisionTenantIdentityCommand;

/** Starts Identity-provider provisioning after a tenant has been committed. */
public interface ProvisionTenantIdentityUseCase {

  void provision(ProvisionTenantIdentityCommand command);
}
