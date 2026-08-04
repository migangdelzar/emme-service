package com.emme.assistant.adapter.in.web.request;

import com.emme.assistant.api.type.ActionTypeView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ProposeActionRequest(
    @NotNull ActionTypeView actionType, @NotBlank String details, @NotNull Instant expiresAt) {}
