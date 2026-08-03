package com.emme.identity.api.query;

import java.util.UUID;

/** Public query for the authenticated user's consolidated Identity view. */
public record GetCurrentUserQuery(
    String userId, String email, String displayName, UUID selectedTenantId) {}
