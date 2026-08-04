package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.result.TenantDetails;
import java.util.List;

public interface ListActiveTenantsUseCase {
  List<TenantDetails> list(ListActiveTenantsQuery query);
}
