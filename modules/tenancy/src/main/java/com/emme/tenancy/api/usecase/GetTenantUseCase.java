package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantInfo;
import java.util.Optional;

public interface GetTenantUseCase {
  Optional<TenantInfo> get(GetTenantQuery query);
}
