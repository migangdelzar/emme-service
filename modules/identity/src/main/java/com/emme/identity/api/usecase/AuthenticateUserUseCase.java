package com.emme.identity.api.usecase;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.query.GetUserClaimsQuery;
import com.emme.identity.api.result.UserClaimsResult;
import com.emme.identity.api.result.UserTokenResult;

/** Public password-grant and user-info capabilities exposed by Identity. */
public interface AuthenticateUserUseCase {

  UserTokenResult authenticate(AuthenticateUserCommand command);

  UserClaimsResult getUserClaims(GetUserClaimsQuery query);
}
