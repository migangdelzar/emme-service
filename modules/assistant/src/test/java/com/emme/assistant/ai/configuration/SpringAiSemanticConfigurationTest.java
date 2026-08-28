package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.SemanticIntentClassifier;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import com.emme.assistant.ai.application.semantic.SemanticMatchPolicy;
import org.junit.jupiter.api.Test;

class SpringAiSemanticConfigurationTest {

  @Test
  void buildsSemanticClassificationFromTheProviderNeutralPorts() {
    SpringAiSemanticConfiguration configuration = new SpringAiSemanticConfiguration();
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    SemanticMatchPolicy policy = new SemanticMatchPolicy(0.88, 0.12);

    SemanticIntentClassifier classifier = configuration.semanticIntentClassifier(search, policy);
    SemanticIntentRouter router =
        configuration.semanticIntentRouter(
            embeddings, classifier, new SemanticRoutingProperties(true, "es-MX", 0.88, 0.12));

    assertThat(classifier).isNotNull();
    assertThat(router).isNotNull();
  }

  @Test
  void suppliesConservativeDefaults() {
    SemanticRoutingProperties properties = new SemanticRoutingProperties(true, null, null, null);

    assertThat(properties.locale()).isEqualTo("es-MX");
    assertThat(properties.minimumTop1Similarity()).isEqualTo(0.90);
    assertThat(properties.minimumMargin()).isEqualTo(0.10);
  }
}
