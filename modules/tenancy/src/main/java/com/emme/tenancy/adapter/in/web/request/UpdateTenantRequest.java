package com.emme.tenancy.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP request to rename a tenant. */
public record UpdateTenantRequest(@NotBlank @Size(max = 150) String name) {}
