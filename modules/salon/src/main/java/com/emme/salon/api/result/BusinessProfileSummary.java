package com.emme.salon.api.result;

import java.util.UUID;

public record BusinessProfileSummary(UUID tenantId, String displayName, String locale) {}
