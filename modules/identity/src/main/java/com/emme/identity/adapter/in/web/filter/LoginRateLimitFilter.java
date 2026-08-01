package com.emme.identity.adapter.in.web.filter;

import com.emme.identity.application.port.out.LoginAttemptRateLimiter;
import com.emme.identity.configuration.IdentityRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiter for the login endpoint.
 *
 * <p>Limits POST /api/auth/login to 5 attempts per IP per 60-second sliding window.
 *
 * <p>Attempt state is delegated to an application-owned port so deployments can use atomic,
 * distributed Redis state while local tests and non-Redis environments retain a safe fallback.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String KEY_PREFIX = "identity:login-rate-limit:";

  private final IdentityRateLimitProperties properties;
  private final LoginAttemptRateLimiter rateLimiter;
  private final List<IpAddressMatcher> trustedProxyMatchers;

  public LoginRateLimitFilter(
      IdentityRateLimitProperties properties, LoginAttemptRateLimiter rateLimiter) {
    this.properties = properties;
    this.rateLimiter = rateLimiter;
    this.trustedProxyMatchers =
        properties.getTrustedProxies().stream().map(IpAddressMatcher::new).toList();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    boolean isLogin =
        "/api/auth/login".equals(request.getServletPath())
            && "POST".equalsIgnoreCase(request.getMethod());
    return !isLogin;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String ip = getClientIp(request);
    boolean allowed =
        rateLimiter.tryAcquire(
            KEY_PREFIX + ip, properties.getMaxAttempts(), properties.getWindowMs());

    if (!allowed) {
      response.setStatus(429);
      response.setContentType("application/problem+json");
      response
          .getWriter()
          .write(
              """
              {"type":"about:blank","title":"Too Many Requests",\
              "status":429,"detail":"Too many login attempts. Try again later."}""");
      return;
    }

    chain.doFilter(request, response);
  }

  private String getClientIp(HttpServletRequest request) {
    String remoteAddress = request.getRemoteAddr();
    if (!isTrustedProxy(remoteAddress)) {
      return remoteAddress;
    }

    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return remoteAddress;
  }

  private boolean isTrustedProxy(String remoteAddress) {
    return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddress));
  }
}
