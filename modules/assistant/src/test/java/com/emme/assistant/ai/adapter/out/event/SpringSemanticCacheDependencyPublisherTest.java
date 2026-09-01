package com.emme.assistant.ai.adapter.out.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SpringSemanticCacheDependencyPublisherTest {

  @Test
  void publishesDependencyChangesThroughTheSpringEventBoundary() {
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    SpringSemanticCacheDependencyPublisher publisher =
        new SpringSemanticCacheDependencyPublisher(events);
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            SemanticCacheDependencyChanged.Dependency.PRICE,
            "price-v2",
            Instant.parse("2026-08-31T12:00:00Z"));

    publisher.publish(event);

    verify(events).publishEvent(event);
  }
}
