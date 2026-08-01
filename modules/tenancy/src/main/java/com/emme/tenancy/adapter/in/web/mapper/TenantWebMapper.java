package com.emme.tenancy.adapter.in.web.mapper;

import com.emme.tenancy.adapter.in.web.response.TenantResponse;
import com.emme.tenancy.api.result.TenantInfo;
import org.springframework.stereotype.Component;

/** Maps Tenancy domain results to HTTP response representations. */
@Component
public final class TenantWebMapper {

  public TenantResponse toResponse(TenantInfo tenant) {
    return new TenantResponse(
        tenant.id(), tenant.slug(), tenant.name(), tenant.status(), tenant.createdAt());
  }
}
