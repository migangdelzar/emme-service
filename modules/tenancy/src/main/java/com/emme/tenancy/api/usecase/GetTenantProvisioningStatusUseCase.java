package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.GetTenantProvisioningStatusQuery;
import com.emme.tenancy.api.result.TenantProvisioningStatus;

public interface GetTenantProvisioningStatusUseCase {
  TenantProvisioningStatus get(GetTenantProvisioningStatusQuery query);
}
