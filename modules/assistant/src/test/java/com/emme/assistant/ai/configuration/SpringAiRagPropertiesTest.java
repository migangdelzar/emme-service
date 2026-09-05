package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SpringAiRagPropertiesTest {

  @Test
  void suppliesAConservativeRetrievalLimitWhenUnconfigured() {
    assertThat(new SpringAiRagProperties(true, 0).retrievalLimit()).isEqualTo(5);
  }

  @Test
  void rejectsAnUnsafeRetrievalLimit() {
    assertThatThrownBy(() -> new SpringAiRagProperties(true, 21))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("retrievalLimit must be between 1 and 20");
  }

  @Test
  void exposesIndependentQualityPoliciesForEachKnowledgeRoute() {
    var properties =
        new SpringAiRagProperties(
            true,
            5,
            new SpringAiRagProperties.Quality(
                routePolicy(0.80), routePolicy(0.70), routePolicy(0.75), routePolicy(0.65)));

    assertThat(properties.quality().policy(KnowledgeRoute.FAQ).minimumTopScore()).isEqualTo(0.80);
    assertThat(properties.quality().policy(KnowledgeRoute.POLICY).minimumTopScore())
        .isEqualTo(0.70);
  }

  @Test
  void rejectsInvalidRouteQualityConfiguration() {
    assertThatThrownBy(
            () ->
                new SpringAiRagProperties.Quality(
                    routePolicy(Double.NaN),
                    routePolicy(0.70),
                    routePolicy(0.75),
                    routePolicy(0.65)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minimumTopScore must be between 0 and 1");
  }

  private static SpringAiRagProperties.RoutePolicy routePolicy(double minimumTopScore) {
    return new SpringAiRagProperties.RoutePolicy(
        minimumTopScore, 0.10, 1, Duration.ofDays(365), false);
  }
}
