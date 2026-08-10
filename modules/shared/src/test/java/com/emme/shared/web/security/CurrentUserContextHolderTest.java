package com.emme.shared.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserContextHolderTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsTheAuthenticatedSubject() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user-123", "credentials"));

    assertThat(CurrentUserContextHolder.currentSubject()).isEqualTo("user-123");
  }

  @Test
  void rejectsMissingAuthentication() {
    assertThatThrownBy(CurrentUserContextHolder::currentSubject)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No authenticated user context");
  }
}
