package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;

public interface CreateTenantUseCase {
  TenantInfo create(CreateTenantCommand command);
}
