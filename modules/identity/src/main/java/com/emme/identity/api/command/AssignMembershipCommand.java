package com.emme.identity.api.command;

import java.util.UUID;

/** Public intent to assign a role-backed membership to a user for a tenant. */
public record AssignMembershipCommand(UUID tenantId, UUID roleId, String userReference) {}
