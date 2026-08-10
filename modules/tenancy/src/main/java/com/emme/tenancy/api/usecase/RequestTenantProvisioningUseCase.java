package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.command.RequestTenantProvisioningCommand;
import java.util.UUID;

public interface RequestTenantProvisioningUseCase {
  UUID request(RequestTenantProvisioningCommand command);
}
