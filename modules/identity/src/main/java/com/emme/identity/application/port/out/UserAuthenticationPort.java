package com.emme.identity.application.port.out;

import com.emme.identity.api.result.UserClaimsResult;
import com.emme.identity.api.result.UserTokenResult;

/** External authentication capability required by user authentication use cases. */
public interface UserAuthenticationPort {

  UserTokenResult authenticate(String realm, String username, String password);

  UserClaimsResult getUserClaims(String accessToken);
}
