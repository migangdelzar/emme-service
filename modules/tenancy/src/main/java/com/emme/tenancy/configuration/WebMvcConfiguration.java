package com.emme.tenancy.configuration;

import com.emme.tenancy.adapter.in.web.filter.TenantRateLimitInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers optional Tenancy HTTP pipeline concerns for API paths. */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final ObjectProvider<TenantRateLimitInterceptor> rateLimitInterceptor;

  public WebMvcConfiguration(ObjectProvider<TenantRateLimitInterceptor> rateLimitInterceptor) {
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
