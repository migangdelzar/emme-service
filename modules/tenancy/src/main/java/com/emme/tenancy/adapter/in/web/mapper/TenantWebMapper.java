package com.emme.tenancy.adapter.in.web.mapper;

import com.emme.tenancy.adapter.in.web.response.TenantResponse;
import com.emme.tenancy.domain.model.Tenant;
import org.springframework.stereotype.Component;

/** Maps Tenancy domain results to HTTP response representations. */
@Component
public final class TenantWebMapper {

  public TenantResponse toResponse(Tenant tenant) {
    return new TenantResponse(
        tenant.id(), tenant.slug(), tenant.name(), tenant.status().name(), tenant.createdAt());
  }
}
