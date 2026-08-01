package com.emme.tenancy.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP request to rename a tenant. */
public record UpdateTenantRequest(@NotBlank String name) {}
