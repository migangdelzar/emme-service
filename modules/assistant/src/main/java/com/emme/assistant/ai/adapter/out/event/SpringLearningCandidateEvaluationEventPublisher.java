package com.emme.assistant.ai.adapter.out.event;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;
import com.emme.ai.platform.learning.LearningCandidateEvaluationRequester;
import com.emme.assistant.api.event.LearningCandidateEvaluationRequested;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Publishes candidate evaluation through Spring Modulith's durable event boundary. */
public final class SpringLearningCandidateEvaluationEventPublisher
    implements LearningCandidateEvaluationRequester {

  private final ApplicationEventPublisher events;

  public SpringLearningCandidateEvaluationEventPublisher(ApplicationEventPublisher events) {
    this.events = Objects.requireNonNull(events, "events must not be null");
  }

  @Override
  public void request(LearningCandidateEvaluationRequest request) {
    events.publishEvent(new LearningCandidateEvaluationRequested(request));
  }
}
