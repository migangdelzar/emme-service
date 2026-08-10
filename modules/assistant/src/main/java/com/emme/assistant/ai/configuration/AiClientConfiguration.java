package com.emme.assistant.ai.configuration;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Framework wiring for shared AI transport clients. */
@Configuration(proxyBeanMethods = false)
public class AiClientConfiguration {

  @Bean
  AiHttpClient aiHttpClient() {
    return new AiHttpClient(new OkHttpClient());
  }
}
