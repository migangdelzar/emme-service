package com.emme.assistant.adapter.in.web.request;

import com.emme.assistant.domain.model.ActionType;
import java.time.Instant;

public record ProposeActionRequest(ActionType actionType, String details, Instant expiresAt) {}
