package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.result.TenantInfo;
import java.util.List;

public interface ListActiveTenantsUseCase {
  List<TenantInfo> list(ListActiveTenantsQuery query);
}
