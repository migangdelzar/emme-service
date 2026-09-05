package com.emme.notification.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class NotificationClientConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(NotificationClientConfiguration.class)
          .withBean(RestClient.Builder.class, RestClient::builder);

  @Test
  void exposesOneCapabilityScopedRestClient() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasBean("notificationRestClient");
          assertThat(context).getBean("notificationRestClient").isInstanceOf(RestClient.class);
        });
  }
}
