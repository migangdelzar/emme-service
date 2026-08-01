package com.emme.identity.application.port.out;

import com.emme.identity.api.result.UserInfoResult;
import com.emme.identity.api.result.UserTokenResult;

/** External authentication capability required by user authentication use cases. */
public interface UserAuthenticationPort {

  UserTokenResult authenticate(String realm, String username, String password);

  UserInfoResult getUserInfo(String accessToken);
}
