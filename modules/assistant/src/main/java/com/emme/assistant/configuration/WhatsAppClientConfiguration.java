package com.emme.assistant.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition-root wiring for the WhatsApp Cloud API transport. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
public class WhatsAppClientConfiguration {

  @Bean(name = "whatsappRestClient")
  RestClient whatsappRestClient(RestClient.Builder builder, WhatsAppProperties properties) {
    return builder.baseUrl(properties.apiBaseUrl()).build();
  }
}
