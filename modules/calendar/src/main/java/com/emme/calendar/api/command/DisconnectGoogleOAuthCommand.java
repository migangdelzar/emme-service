package com.emme.calendar.api.command;

import com.emme.calendar.api.type.GoogleOAuthPersona;
import java.util.UUID;

/** Revokes and removes Google OAuth credentials for a Calendar persona. */
public record DisconnectGoogleOAuthCommand(
    UUID tenantId, String userId, GoogleOAuthPersona persona) {}
