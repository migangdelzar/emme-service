package com.emme.tenancy.api.result;

import java.time.Instant;

public record TenantProvisioningStatus(
    String status, String schemaName, Instant lastMigratedAt, String error) {}
