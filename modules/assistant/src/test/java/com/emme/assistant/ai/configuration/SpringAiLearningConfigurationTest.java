package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.learning.JdbcLearningCandidateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiLearningConfigurationTest {

  @Test
  void createsTheCandidateStoreFromTheExistingTenantAwareJdbcClient() {
    JdbcClient jdbc = mock(JdbcClient.class);

    assertThat(new SpringAiLearningConfiguration().learningCandidateStore(jdbc, new ObjectMapper()))
        .isInstanceOf(JdbcLearningCandidateStore.class);
  }
}
