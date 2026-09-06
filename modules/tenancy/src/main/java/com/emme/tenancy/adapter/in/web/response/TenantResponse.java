package com.emme.tenancy.adapter.in.web.response;

import com.emme.tenancy.domain.model.TenantStatus;
import java.time.Instant;
import java.util.UUID;

/** HTTP response representation of a tenant. */
public record TenantResponse(
    UUID id, String slug, String name, TenantStatus status, Instant createdAt) {}
