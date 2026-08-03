package com.emme.studio.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP request for creating a Studio customer. */
public record CreateCustomerRequest(@NotBlank String name, String phone, String email) {}
