package com.emme.ai.platform.configuration;

import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for provider configuration and transport dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiProviderConfiguration {

  @Bean
  AiProviderHttpClient aiProviderHttpClient() {
    return new AiProviderHttpClient(new OkHttpClient());
  }
}
