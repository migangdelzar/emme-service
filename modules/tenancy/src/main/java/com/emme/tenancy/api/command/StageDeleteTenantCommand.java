package com.emme.tenancy.api.command;

import java.util.UUID;

public record StageDeleteTenantCommand(UUID tenantId) {}
