package com.emme.tenancy.api.command;

import java.util.UUID;

public record UpdateTenantCommand(UUID tenantId, String name) {}
