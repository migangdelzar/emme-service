package com.emme.tenancy.api.event;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/** Public fact emitted after a tenant is persisted. */
@Externalized("emme.tenancy.tenant-created::#{#this.tenantId()}")
public record TenantCreated(
    UUID eventId, UUID tenantId, String slug, String name, String adminEmail) {}
