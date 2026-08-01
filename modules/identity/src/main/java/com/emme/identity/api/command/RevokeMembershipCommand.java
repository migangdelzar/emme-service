package com.emme.identity.api.command;

import java.util.UUID;

/** Public intent to revoke an existing membership. */
public record RevokeMembershipCommand(UUID membershipId) {}
