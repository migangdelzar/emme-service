package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.StageDeleteTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;

public interface StageDeleteTenantUseCase {
  TenantDetails stageDelete(StageDeleteTenantCommand command);
}
