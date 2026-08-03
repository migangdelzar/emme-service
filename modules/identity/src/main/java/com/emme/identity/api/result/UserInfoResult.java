package com.emme.identity.api.result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Verified user claims returned by the Identity provider user-info endpoint. */
public record UserInfoResult(Map<String, Object> claims) {

  public UserInfoResult {
    claims = Collections.unmodifiableMap(new LinkedHashMap<>(claims));
  }
}
