package com.emme.identity.adapter.in.web.security;

import java.util.UUID;

/** Transport-neutral authenticated user context extracted from a security principal. */
public record UserContext(String subject, String email, String displayName, UUID tenantId) {}
