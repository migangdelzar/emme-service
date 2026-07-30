package com.emme.identity;

import java.util.UUID;

public record UserContext(String subject, String email, String displayName, UUID tenantId) {}
