package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.result.TenantDetails;
import java.util.Optional;

public interface GetTenantUseCase {
  Optional<TenantDetails> get(GetTenantQuery query);
}
