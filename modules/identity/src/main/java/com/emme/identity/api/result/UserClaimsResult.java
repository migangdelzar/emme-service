package com.emme.identity.api.result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Verified user claims returned by the Identity provider user-info endpoint. */
public record UserClaimsResult(Map<String, Object> claims) {

  public UserClaimsResult {
    claims = Collections.unmodifiableMap(new LinkedHashMap<>(claims));
  }
}
