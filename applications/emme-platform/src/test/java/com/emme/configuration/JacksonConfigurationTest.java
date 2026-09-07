package com.emme.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JacksonConfigurationTest {

  @Test
  void createsAnObjectMapperThatSerializesJavaTimeValues() throws Exception {
    ObjectMapper mapper = new JacksonConfiguration().objectMapper();

    assertThat(mapper.writeValueAsString(Instant.parse("2026-09-07T00:00:00Z"))).isNotBlank();
  }
}
