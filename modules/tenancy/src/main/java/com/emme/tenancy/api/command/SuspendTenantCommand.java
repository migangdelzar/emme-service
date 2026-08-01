package com.emme.tenancy.api.command;

import java.util.UUID;

public record SuspendTenantCommand(UUID tenantId) {}
