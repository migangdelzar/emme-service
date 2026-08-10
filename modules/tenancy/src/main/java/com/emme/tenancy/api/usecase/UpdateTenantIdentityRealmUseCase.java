package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.UpdateTenantIdentityRealmCommand;

public interface UpdateTenantIdentityRealmUseCase {
  void update(UpdateTenantIdentityRealmCommand command);
}
