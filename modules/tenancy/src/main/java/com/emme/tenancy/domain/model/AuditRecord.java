package com.emme.tenancy.domain.model;

import java.util.UUID;

public record AuditRecord(
    UUID tenantId, String actorReference, String action, AuditOutcome outcome) {}
