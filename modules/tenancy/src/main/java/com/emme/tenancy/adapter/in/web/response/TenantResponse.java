package com.emme.tenancy.adapter.in.web.response;

import java.time.Instant;
import java.util.UUID;

/** HTTP response representation of a tenant. */
public record TenantResponse(UUID id, String slug, String name, String status, Instant createdAt) {}
