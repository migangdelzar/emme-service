package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.RetrievalQualityPolicy;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/** Explicit settings for the optional Spring AI retrieval-augmented answer path. */
@ConfigurationProperties("app.ai.spring-rag")
public record SpringAiRagProperties(boolean enabled, int retrievalLimit, Quality quality) {

  public SpringAiRagProperties(boolean enabled, int retrievalLimit) {
    this(enabled, retrievalLimit, Quality.defaults());
  }

  @ConstructorBinding
  public SpringAiRagProperties {
    if (retrievalLimit == 0) {
      retrievalLimit = 5;
    }
    if (retrievalLimit < 1 || retrievalLimit > 20) {
      throw new IllegalArgumentException("retrievalLimit must be between 1 and 20");
    }
    quality = quality == null ? Quality.defaults() : quality;
  }

  public record Quality(
      RoutePolicy faq, RoutePolicy policy, RoutePolicy design, RoutePolicy general) {

    public Quality {
      faq = faq == null ? RoutePolicy.defaults() : faq;
      policy = policy == null ? RoutePolicy.defaults() : policy;
      design = design == null ? RoutePolicy.defaults() : design;
      general = general == null ? RoutePolicy.defaults() : general;
    }

    public static Quality defaults() {
      RoutePolicy defaults = RoutePolicy.defaults();
      return new Quality(defaults, defaults, defaults, defaults);
    }

    public RetrievalQualityPolicy policy(KnowledgeRoute route) {
      Objects.requireNonNull(route, "route must not be null");
      return switch (route) {
        case FAQ -> faq.toPolicy();
        case POLICY -> policy.toPolicy();
        case DESIGN -> design.toPolicy();
        case GENERAL -> general.toPolicy();
      };
    }
  }

  public record RoutePolicy(
      double minimumTopScore,
      double minimumMargin,
      int minimumSupportingDocuments,
      Duration maximumDocumentAge,
      boolean requireLexicalAgreement) {

    public RoutePolicy {
      new RetrievalQualityPolicy(
          minimumTopScore,
          minimumMargin,
          minimumSupportingDocuments,
          maximumDocumentAge,
          requireLexicalAgreement);
    }

    public static RoutePolicy defaults() {
      return new RoutePolicy(0.75, 0.10, 1, Duration.ofDays(180), false);
    }

    public RetrievalQualityPolicy toPolicy() {
      return new RetrievalQualityPolicy(
          minimumTopScore,
          minimumMargin,
          minimumSupportingDocuments,
          maximumDocumentAge,
          requireLexicalAgreement);
    }
  }
}
