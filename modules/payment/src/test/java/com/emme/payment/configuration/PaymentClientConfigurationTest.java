package com.emme.payment.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class PaymentClientConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(PaymentClientConfiguration.class)
          .withBean(RestClient.Builder.class, RestClient::builder);

  @Test
  void exposesOneCapabilityScopedRestClient() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasBean("paymentRestClient");
          assertThat(context).getBean("paymentRestClient").isInstanceOf(RestClient.class);
        });
  }
}
