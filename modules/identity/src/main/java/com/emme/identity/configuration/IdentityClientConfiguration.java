package com.emme.identity.configuration;

import com.emme.identity.application.port.out.IdentityRealmConfigurationPort;
import com.emme.identity.application.port.out.RetryDelayPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition-root beans for Identity outbound HTTP clients. */
@Configuration
public class IdentityClientConfiguration {

  @Bean(name = "identityRestClient")
  public RestClient identityRestClient(RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  public RetryDelayPort identityRetryDelayPort() {
    return delay -> Thread.sleep(delay.toMillis());
  }

  @Bean
  public IdentityRealmConfigurationPort identityRealmConfiguration(
      IdentityKeycloakProperties properties) {
    return properties::defaultRealm;
  }
}
