package com.emme.calendar.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition-root wiring for Google adapter transport dependencies. */
@Configuration
public class GoogleClientConfiguration {
  @Bean(name = "googleRestClient")
  public RestClient googleRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
