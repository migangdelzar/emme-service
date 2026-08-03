package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.SuspendTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;

public interface SuspendTenantUseCase {
  TenantInfo suspend(SuspendTenantCommand command);
}
