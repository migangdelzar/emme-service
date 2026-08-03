package com.emme.identity.configuration;

import com.emme.identity.application.port.out.IdentityRealmConfigurationPort;
import com.emme.identity.application.port.out.RetryDelayPort;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root beans for Identity outbound HTTP clients. */
@Configuration
public class IdentityClientConfiguration {

  @Bean
  public OkHttpClient identityHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .build();
  }

  @Bean
  public RetryDelayPort identityRetryDelayPort() {
    return delay -> Thread.sleep(delay.toMillis());
  }

  @Bean
  public IdentityRealmConfigurationPort identityRealmConfiguration(
      IdentityKeycloakProperties properties) {
    return properties::getDefaultRealm;
  }
}
