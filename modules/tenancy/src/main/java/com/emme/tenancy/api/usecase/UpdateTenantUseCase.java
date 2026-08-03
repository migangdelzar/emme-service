package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.UpdateTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;

public interface UpdateTenantUseCase {
  TenantInfo update(UpdateTenantCommand command);
}
