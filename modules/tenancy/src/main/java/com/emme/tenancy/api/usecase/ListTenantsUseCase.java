package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.result.TenantInfo;
import java.util.List;

public interface ListTenantsUseCase {
  List<TenantInfo> list(ListTenantsQuery query);
}
