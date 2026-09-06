package com.emme.tenancy.api.result;

import com.emme.tenancy.domain.model.TenantProvisioningState;
import java.time.Instant;

public record TenantProvisioningStatus(
    TenantProvisioningState status, String schemaName, Instant lastMigratedAt, String error) {}
