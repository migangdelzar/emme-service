package com.emme.identity.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root beans for Identity outbound HTTP clients. */
@Configuration
public class IdentityClientConfiguration {

  @Bean
  public OkHttpClient identityHttpClient() {
    return new OkHttpClient();
  }
}
