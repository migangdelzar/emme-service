package com.emme.studio.api.result;

import java.util.UUID;

public record BusinessProfileInfo(UUID tenantId, String displayName, String locale) {}
