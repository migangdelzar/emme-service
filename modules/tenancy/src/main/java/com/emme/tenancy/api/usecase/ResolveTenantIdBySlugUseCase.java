package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.query.ResolveTenantIdBySlugQuery;
import java.util.UUID;

public interface ResolveTenantIdBySlugUseCase {
  UUID resolve(ResolveTenantIdBySlugQuery query);
}
