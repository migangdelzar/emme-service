package com.emme.identity.api.usecase;

import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.result.CurrentUserInfo;

/** Consolidates the authenticated user's memberships, permissions, and profile. */
public interface GetCurrentUserUseCase {

  CurrentUserInfo get(GetCurrentUserQuery query);
}
