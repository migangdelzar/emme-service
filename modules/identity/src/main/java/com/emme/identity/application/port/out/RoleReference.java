package com.emme.identity.application.port.out;

import java.util.UUID;

/** Stable role data needed by the membership application workflow. */
public record RoleReference(UUID id, String code) {}
