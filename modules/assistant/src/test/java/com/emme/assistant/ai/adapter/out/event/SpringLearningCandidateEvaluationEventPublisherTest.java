package com.emme.assistant.ai.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;
import com.emme.assistant.api.event.LearningCandidateEvaluationRequested;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.Externalized;

class SpringLearningCandidateEvaluationEventPublisherTest {

  @Test
  void publishesOnlyTheTrustedEvaluationEnvelope() {
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    SpringLearningCandidateEvaluationEventPublisher publisher =
        new SpringLearningCandidateEvaluationEventPublisher(events);
    LearningCandidateEvaluationRequest request =
        new LearningCandidateEvaluationRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-1",
            "idem-1");

    publisher.request(request);

    var event = org.mockito.ArgumentCaptor.forClass(LearningCandidateEvaluationRequested.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().request()).isEqualTo(request);
  }

  @Test
  void usesTheExistingDurableModulithPublicationBoundary() {
    Externalized externalized =
        LearningCandidateEvaluationRequested.class.getAnnotation(Externalized.class);

    assertThat(externalized).isNotNull();
    assertThat(externalized.value())
        .isEqualTo("emme.ai.learning-candidate-evaluation-requested::#{#this.tenantId()}");
  }
}
