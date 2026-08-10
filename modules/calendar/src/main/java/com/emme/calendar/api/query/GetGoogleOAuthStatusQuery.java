package com.emme.calendar.api.query;

import com.emme.calendar.api.type.GoogleOAuthPersona;
import java.util.UUID;

/** Requests the current Google OAuth connection state. */
public record GetGoogleOAuthStatusQuery(UUID tenantId, String userId, GoogleOAuthPersona persona) {}
