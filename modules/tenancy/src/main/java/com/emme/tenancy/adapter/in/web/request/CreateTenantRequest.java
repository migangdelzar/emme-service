package com.emme.tenancy.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP request to create a tenant. */
public record CreateTenantRequest(
    @NotBlank @Size(max = 50) String slug, @NotBlank @Size(max = 150) String name) {}
