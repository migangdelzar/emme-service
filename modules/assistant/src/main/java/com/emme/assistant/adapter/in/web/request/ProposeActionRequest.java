package com.emme.assistant.adapter.in.web.request;

import com.emme.assistant.api.type.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ProposeActionRequest(
    @NotNull ActionType actionType, @NotBlank String details, @NotNull Instant expiresAt) {}
