package com.emme.payment.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition-root wiring for payment provider transport dependencies. */
@Configuration
public class PaymentClientConfiguration {
  @Bean(name = "paymentRestClient")
  public RestClient paymentRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
