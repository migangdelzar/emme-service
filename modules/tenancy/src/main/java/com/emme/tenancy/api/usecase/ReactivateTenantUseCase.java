package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.ReactivateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;

public interface ReactivateTenantUseCase {
  TenantInfo reactivate(ReactivateTenantCommand command);
}
