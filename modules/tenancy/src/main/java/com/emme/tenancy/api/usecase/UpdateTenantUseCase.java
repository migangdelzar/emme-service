package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.UpdateTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;

public interface UpdateTenantUseCase {
  TenantDetails update(UpdateTenantCommand command);
}
