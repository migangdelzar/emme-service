package com.emme.tenancy.api.command;

import java.util.UUID;

public record UpdateTenantIdentityRealmCommand(UUID tenantId, String identityRealm) {}
