package com.emme.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Shared Jackson 2 mapper used by HTTP, provider, and Kafka event adapters. */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
