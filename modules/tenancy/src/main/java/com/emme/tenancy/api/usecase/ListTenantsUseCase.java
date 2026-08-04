package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.result.TenantDetails;
import java.util.List;

public interface ListTenantsUseCase {
  List<TenantDetails> list(ListTenantsQuery query);
}
