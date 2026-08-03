package com.emme.identity.api.usecase;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.query.GetUserInfoQuery;
import com.emme.identity.api.result.UserInfoResult;
import com.emme.identity.api.result.UserTokenResult;

/** Public password-grant and user-info capabilities exposed by Identity. */
public interface AuthenticateUserUseCase {

  UserTokenResult authenticate(AuthenticateUserCommand command);

  UserInfoResult getUserInfo(GetUserInfoQuery query);
}
