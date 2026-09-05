package com.emme.calendar.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class GoogleClientConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(GoogleClientConfiguration.class)
          .withBean(RestClient.Builder.class, RestClient::builder);

  @Test
  void exposesOneCapabilityScopedRestClient() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasBean("googleRestClient");
          assertThat(context).getBean("googleRestClient").isInstanceOf(RestClient.class);
        });
  }
}
