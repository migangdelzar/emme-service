package com.emme.identity.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityAuditLoggerTest {

  @Test
  void exposesOnlyTheAuthenticationFailureTypeAsTheAuditReason() {
    var exception = new BadCredentialsException("sensitive-provider-detail");

    assertThat(SecurityAuditLogger.safeFailureReason(exception))
        .isEqualTo("BadCredentialsException");
  }

  @Test
  void removesControlCharactersAndBoundsAuditValues() {
    String input = "principal\nwith\rcontrol\t" + "x".repeat(300);

    String sanitized = SecurityAuditLogger.safeLogValue(input);

    assertThat(sanitized).doesNotContain("\n", "\r", "\t");
    assertThat(sanitized).hasSize(256);
  }

  @Test
  void usesTheSocketAddressInsteadOfAnUntrustedForwardedHeader() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "203.0.113.50");

    assertThat(SecurityAuditLogger.clientIp(request)).isEqualTo("10.0.0.10");
  }

  @Test
  void sanitizesTheRequestUriBeforeItIsWrittenToAuditLogs() {
    var request = new MockHttpServletRequest();
    request.setRequestURI("/api/unsafe\npath");

    assertThat(SecurityAuditLogger.requestUri(request)).isEqualTo("/api/unsafe?path");
  }
}
