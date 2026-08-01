package com.emme.identity.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.configuration.IdentityRateLimitProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

  @Test
  void ignoresForwardedClientIpWhenImmediatePeerIsNotTrusted() throws Exception {
    IdentityRateLimitProperties properties = properties(1, List.of());
    LoginRateLimitFilter filter = new LoginRateLimitFilter(properties);

    MockHttpServletRequest first = loginRequest("10.0.0.5", "203.0.113.10");
    MockHttpServletResponse firstResponse = new MockHttpServletResponse();
    filter.doFilter(first, firstResponse, new MockFilterChain());

    MockHttpServletRequest rotatedHeader = loginRequest("10.0.0.5", "203.0.113.11");
    MockHttpServletResponse rotatedHeaderResponse = new MockHttpServletResponse();
    filter.doFilter(rotatedHeader, rotatedHeaderResponse, new MockFilterChain());

    assertThat(firstResponse.getStatus()).isEqualTo(200);
    assertThat(rotatedHeaderResponse.getStatus()).isEqualTo(429);
  }

  @Test
  void usesForwardedClientIpWhenImmediatePeerMatchesTrustedNetwork() throws Exception {
    IdentityRateLimitProperties properties = properties(1, List.of("10.0.0.0/8"));
    LoginRateLimitFilter filter = new LoginRateLimitFilter(properties);

    MockHttpServletResponse firstResponse = new MockHttpServletResponse();
    filter.doFilter(loginRequest("10.0.0.5", "203.0.113.10"), firstResponse, new MockFilterChain());

    MockHttpServletResponse sameClientResponse = new MockHttpServletResponse();
    filter.doFilter(
        loginRequest("10.0.0.5", "203.0.113.10"), sameClientResponse, new MockFilterChain());

    MockHttpServletResponse differentClientResponse = new MockHttpServletResponse();
    filter.doFilter(
        loginRequest("10.0.0.5", "203.0.113.11"), differentClientResponse, new MockFilterChain());

    assertThat(firstResponse.getStatus()).isEqualTo(200);
    assertThat(sameClientResponse.getStatus()).isEqualTo(429);
    assertThat(differentClientResponse.getStatus()).isEqualTo(200);
  }

  private static IdentityRateLimitProperties properties(
      int maxAttempts, List<String> trustedProxies) {
    IdentityRateLimitProperties properties = new IdentityRateLimitProperties();
    properties.setMaxAttempts(maxAttempts);
    properties.setTrustedProxies(trustedProxies);
    return properties;
  }

  private static MockHttpServletRequest loginRequest(String remoteAddress, String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath("/api/auth/login");
    request.setMethod("POST");
    request.setRemoteAddr(remoteAddress);
    request.addHeader("X-Forwarded-For", forwardedFor);
    return request;
  }
}
