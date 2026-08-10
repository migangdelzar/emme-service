package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;

public interface CreateTenantUseCase {
  TenantDetails create(CreateTenantCommand command);
}
