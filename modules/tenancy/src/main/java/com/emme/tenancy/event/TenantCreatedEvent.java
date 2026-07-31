package com.emme.tenancy.event;

import java.util.UUID;

/**
 * Fired after a tenant is successfully persisted. Listeners (e.g. KeycloakRealmProvisioner) react
 * to this.
 */
public record TenantCreatedEvent(UUID tenantId, String slug, String name, String adminEmail) {}
