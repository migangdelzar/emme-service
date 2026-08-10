package com.emme.calendar.api.command;

import com.emme.calendar.api.type.GoogleOAuthPersona;

/** Starts a Google OAuth consent flow for a Calendar persona. */
public record StartGoogleOAuthCommand(GoogleOAuthPersona persona, String state) {}
