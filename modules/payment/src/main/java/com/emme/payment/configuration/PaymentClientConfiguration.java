package com.emme.payment.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root wiring for payment provider transport dependencies. */
@Configuration
public class PaymentClientConfiguration {
  @Bean
  public PaymentHttpClient paymentHttpClient() {
    return new PaymentHttpClient(new OkHttpClient());
  }
}
