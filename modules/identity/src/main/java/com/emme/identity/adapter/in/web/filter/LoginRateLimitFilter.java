package com.emme.identity.adapter.in.web.filter;

import com.emme.identity.configuration.IdentityRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory rate limiter for the login endpoint.
 *
 * <p>Limits POST /api/auth/login to 5 attempts per IP per 60-second sliding window.
 *
 * <p>The map grows unbounded — acceptable for MVP enterprise hardening since login endpoints are
 * low-traffic and IP diversity is bounded by the client base.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private final IdentityRateLimitProperties properties;
  private final List<IpAddressMatcher> trustedProxyMatchers;

  /** IP → list of attempt timestamps (epoch ms). */
  private final ConcurrentHashMap<String, List<Long>> attempts = new ConcurrentHashMap<>();

  public LoginRateLimitFilter(IdentityRateLimitProperties properties) {
    this.properties = properties;
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
    long now = System.currentTimeMillis();

    List<Long> timestamps =
        attempts.computeIfAbsent(ip, k -> Collections.synchronizedList(new ArrayList<>()));

    synchronized (timestamps) {
      timestamps.removeIf(ts -> now - ts > properties.getWindowMs());

      if (timestamps.size() >= properties.getMaxAttempts()) {
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

      timestamps.add(now);
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
