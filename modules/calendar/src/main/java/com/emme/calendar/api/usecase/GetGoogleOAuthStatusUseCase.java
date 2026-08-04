package com.emme.calendar.api.usecase;

import com.emme.calendar.api.query.GetGoogleOAuthStatusQuery;
import com.emme.calendar.api.result.GoogleOAuthConnectionStatus;

/** Reads the Google OAuth connection state. */
public interface GetGoogleOAuthStatusUseCase {

  GoogleOAuthConnectionStatus get(GetGoogleOAuthStatusQuery query);
}
