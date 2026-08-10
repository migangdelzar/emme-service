package com.emme.identity.api.usecase;

import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.result.CurrentUserDetails;

/** Consolidates the authenticated user's memberships, permissions, and profile. */
public interface GetCurrentUserUseCase {

  CurrentUserDetails get(GetCurrentUserQuery query);
}
