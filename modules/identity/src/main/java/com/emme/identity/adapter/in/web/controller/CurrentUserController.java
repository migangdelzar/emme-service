package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.IdentityWebMapper;
import com.emme.identity.adapter.in.web.response.CurrentUserResponse;
import com.emme.identity.adapter.in.web.security.UserContext;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.usecase.GetCurrentUserUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

  private final GetCurrentUserUseCase getCurrentUser;

  public CurrentUserController(GetCurrentUserUseCase getCurrentUser) {
    this.getCurrentUser = getCurrentUser;
  }

  @GetMapping(path = "/api/me", version = "1.0")
  public CurrentUserResponse currentUser(@AuthenticationPrincipal Object principal) {
    UserContext user = UserContextHolder.fromPrincipal(principal);
    return IdentityWebMapper.toCurrentUserResponse(
        getCurrentUser.get(
            new GetCurrentUserQuery(
                user.subject(), user.email(), user.displayName(), user.tenantId())));
  }
}
