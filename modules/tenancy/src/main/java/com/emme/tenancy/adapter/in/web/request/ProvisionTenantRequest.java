package com.emme.tenancy.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP request to start asynchronous tenant provisioning. */
public record ProvisionTenantRequest(
    @NotBlank @Size(min = 1, max = 50) String slug,
    @NotBlank @Size(min = 1, max = 150) String name,
    String timeZone,
    String locale) {}
