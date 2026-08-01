package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.StageDeleteTenantCommand;
import com.emme.tenancy.api.result.TenantInfo;

public interface StageDeleteTenantUseCase {
  TenantInfo stageDelete(StageDeleteTenantCommand command);
}
