package com.emme.identity.api.result;

import java.util.UUID;

/** Stable Identity-owned summary of the selected tenant's business profile. */
public record BusinessProfileSummary(UUID tenantId, String displayName, String locale) {}
