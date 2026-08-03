package com.emme.calendar.api.command;

import com.emme.calendar.api.type.GoogleOAuthPersona;
import java.util.UUID;

/** Completes a Google OAuth flow and stores the resulting credentials. */
public record CompleteGoogleOAuthCommand(
    UUID tenantId, String userId, GoogleOAuthPersona persona, String code) {}
