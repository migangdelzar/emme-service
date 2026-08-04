package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.ReactivateTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;

public interface ReactivateTenantUseCase {
  TenantDetails reactivate(ReactivateTenantCommand command);
}
