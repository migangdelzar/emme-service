package com.emme.tenancy.web;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Per-tenant IP-based rate limiting interceptor.
 *
 * <p>Uses Redis atomic increment to track request counts in a sliding time window. Returns HTTP 429
 * with RFC 9457 Problem Detail when the limit is exceeded.
 *
 * <p>Disabled automatically when Redis is unavailable or when {@code app.rate-limit.enabled=false}.
 */
@Component
@ConditionalOnBean(RedisTemplate.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
  private static final String KEY_PREFIX = "rate_limit:";

  private final RedisTemplate<String, String> redis;
  private final RateLimitProperties properties;
  private final ObjectMapper objectMapper;

  public RateLimitInterceptor(
      RedisTemplate<String, String> redis,
      RateLimitProperties properties,
      ObjectMapper objectMapper) {
    this.redis = redis;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    String clientId = resolveClientId(request);
    String key = KEY_PREFIX + clientId;
    Duration window = properties.window();
    int maxRequests = properties.maxRequests();

    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1) {
      redis.expire(key, window);
    }

    if (count != null && count > maxRequests) {
      Long ttl = redis.getExpire(key);
      log.warn(
          "Rate limit exceeded: client={} requests={}/{}, ttl={}s",
          clientId,
          count,
          maxRequests,
          ttl);

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/problem+json");

      Map<String, Object> problem = new LinkedHashMap<>();
      problem.put("type", "about:blank");
      problem.put("title", "Too Many Requests");
      problem.put("status", 429);
      problem.put(
          "detail",
          "Rate limit of %d requests per %ds exceeded. Retry after %ds."
              .formatted(maxRequests, window.toSeconds(), ttl != null ? ttl : 0));
      problem.put("instance", request.getRequestURI());
      problem.put("requestId", CorrelationId.get());
      problem.put("retryAfterSeconds", ttl != null ? ttl : 0);

      objectMapper.writeValue(response.getWriter(), problem);
      return false;
    }

    return true;
  }

  /** Builds the composite rate-limit key from tenant ID and client IP. */
  private String resolveClientId(HttpServletRequest request) {
    String tenantId =
        TenantContextHolder.currentTenantOptional().map(UUID::toString).orElse("anonymous");
    String ip = request.getRemoteAddr();
    return tenantId + ":" + ip;
  }
}
