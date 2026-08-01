package com.emme.identity.api.usecase;

import com.emme.tenancy.api.event.TenantCreated;

/** Starts Identity-provider provisioning after a tenant has been committed. */
public interface ProvisionTenantIdentityUseCase {

  void provision(TenantCreated event);
}
