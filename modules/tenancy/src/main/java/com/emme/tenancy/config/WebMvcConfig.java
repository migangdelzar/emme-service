package com.emme.tenancy.config;

import com.emme.tenancy.web.RateLimitInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link RateLimitInterceptor} for API paths.
 *
 * <p>Actuator, API docs, and Swagger UI endpoints are excluded from rate limiting to avoid blocking
 * operational tooling.
 *
 * <p>The interceptor is resolved lazily via {@link ObjectProvider}: contexts that do not register
 * it (e.g. module test slices without the web layer) simply run without rate limiting instead of
 * failing to start.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final ObjectProvider<RateLimitInterceptor> rateLimitInterceptor;

  public WebMvcConfig(ObjectProvider<RateLimitInterceptor> rateLimitInterceptor) {
    this.rateLimitInterceptor = rateLimitInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    rateLimitInterceptor.ifAvailable(
        interceptor ->
            registry
                .addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/actuator/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"));
  }
}
