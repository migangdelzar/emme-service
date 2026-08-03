package com.emme.identity.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** Credentials submitted to the staff login endpoint. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
