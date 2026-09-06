package com.emme.calendar.configuration;

import okhttp3.OkHttpClient;
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

  /** Compatibility transport retained until the Calendar sync adapters migrate in HTTP-12. */
  @Bean
  public GoogleHttpClient googleHttpClient() {
    return new GoogleHttpClient(new OkHttpClient());
  }
}
