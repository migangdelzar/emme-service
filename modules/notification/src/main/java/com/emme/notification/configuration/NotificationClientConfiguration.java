package com.emme.notification.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root wiring for notification provider transport dependencies. */
@Configuration
public class NotificationClientConfiguration {
  @Bean
  public NotificationHttpClient notificationHttpClient() {
    return new NotificationHttpClient(new OkHttpClient());
  }
}
