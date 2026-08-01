package com.emme.tenancy.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP request to create a tenant. */
public record CreateTenantRequest(@NotBlank String slug, @NotBlank String name) {}
