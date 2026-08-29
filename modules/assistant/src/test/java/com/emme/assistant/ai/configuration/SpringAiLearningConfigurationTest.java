package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.learning.JdbcLearningCandidateStore;
import com.emme.ai.platform.learning.LearningCandidateEvaluationRequester;
import com.emme.ai.platform.learning.LearningCandidateLifecyclePolicy;
import com.emme.ai.platform.learning.LearningCandidateLifecycleService;
import com.emme.ai.platform.learning.LearningCandidateStateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiLearningConfigurationTest {

  @Test
  void createsTheCandidateStoreFromTheExistingTenantAwareJdbcClient() {
    JdbcClient jdbc = mock(JdbcClient.class);

    assertThat(new SpringAiLearningConfiguration().learningCandidateStore(jdbc, new ObjectMapper()))
        .isInstanceOf(JdbcLearningCandidateStore.class);
  }

  @Test
  void wiresTheDeterministicLifecycleServiceAgainstTheStateStore() {
    SpringAiLearningConfiguration configuration = new SpringAiLearningConfiguration();
    LearningCandidateLifecyclePolicy policy = configuration.learningCandidateLifecyclePolicy();
    LearningCandidateStateStore stateStore = mock(LearningCandidateStateStore.class);

    assertThat(configuration.learningCandidateLifecycleService(policy, stateStore))
        .isInstanceOf(LearningCandidateLifecycleService.class);
  }

  @Test
  void wiresCandidateEvaluationDispatchThroughTheApplicationEventBoundary() {
    SpringAiLearningConfiguration configuration = new SpringAiLearningConfiguration();

    assertThat(
            configuration.learningCandidateEvaluationRequester(
                mock(ApplicationEventPublisher.class)))
        .isInstanceOf(LearningCandidateEvaluationRequester.class);
  }
}
