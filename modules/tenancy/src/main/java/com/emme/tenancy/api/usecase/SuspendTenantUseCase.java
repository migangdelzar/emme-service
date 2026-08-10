package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.SuspendTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;

public interface SuspendTenantUseCase {
  TenantDetails suspend(SuspendTenantCommand command);
}
