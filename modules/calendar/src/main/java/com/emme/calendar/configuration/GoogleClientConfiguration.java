package com.emme.calendar.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root wiring for Google adapter transport dependencies. */
@Configuration
public class GoogleClientConfiguration {
  @Bean
  public GoogleHttpClient googleHttpClient() {
    return new GoogleHttpClient(new OkHttpClient());
  }
}
