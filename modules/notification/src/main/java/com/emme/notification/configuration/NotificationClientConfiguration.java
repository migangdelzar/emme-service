package com.emme.notification.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition-root wiring for notification provider transport dependencies. */
@Configuration
public class NotificationClientConfiguration {
  @Bean(name = "notificationRestClient")
  public RestClient notificationRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
