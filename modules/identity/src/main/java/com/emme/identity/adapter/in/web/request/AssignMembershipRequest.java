package com.emme.identity.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Input used to assign a role membership to a user within a tenant. */
public record AssignMembershipRequest(
    @NotNull UUID tenantId, @NotNull UUID roleId, @NotBlank String userReference) {}
