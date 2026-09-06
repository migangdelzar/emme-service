package com.emme.assistant.ai.application.tool;

import static com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.SemanticMatch;
import com.emme.assistant.ai.application.semantic.SemanticMatchPolicy;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.semantic.SemanticToolSelector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SemanticProactiveToolRouterTest {

  private static final EmbeddingVector QUERY = testEmbedding("embedding-v1", List.of(1.0f, 0.0f));

  @Test
  void exposesOnlyThePreparedSemanticQueryRoute() {
    assertThat(
            java.util.Arrays.stream(ProactiveToolRouter.class.getMethods())
                .filter(method -> method.getName().equals("route"))
                .map(method -> method.getParameterTypes()[0]))
        .containsExactly(SemanticQuery.class);
  }

  @Test
  void invokesTheSelectedReadOnlyToolWhenSimilarityAndAuthorizationPass() {
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    when(search.searchTools("es-MX", QUERY, Set.of("getSalonServices"), 2))
        .thenReturn(List.of(new SemanticMatch("getSalonServices", 0.98)));
    AiToolGateway gateway = mock(AiToolGateway.class);
    when(gateway.proactivelyEligibleToolKeys()).thenReturn(Set.of("getSalonServices"));
    when(gateway.execute(
            new AiToolInvocation("getSalonServices", java.util.Map.of(), false, false)))
        .thenReturn(new AiToolResult("getSalonServices", "services", true));
    SemanticProactiveToolRouter router =
        new SemanticProactiveToolRouter(
            new SemanticToolSelector(search, new SemanticMatchPolicy(0.90, 0.10)),
            gateway,
            "es-MX");

    Optional<AiToolResult> result =
        AiExecutionContextScope.call(
            context(), () -> router.route(new SemanticQuery("what services do you have?", QUERY)));

    assertThat(result).contains(new AiToolResult("getSalonServices", "services", true));
    verify(gateway)
        .execute(new AiToolInvocation("getSalonServices", java.util.Map.of(), false, false));
  }

  @Test
  void abstainsWithoutEligibleToolsBeforeEmbeddingTheMessage() {
    AiToolGateway gateway = mock(AiToolGateway.class);
    when(gateway.proactivelyEligibleToolKeys()).thenReturn(Set.of());
    SemanticProactiveToolRouter router =
        new SemanticProactiveToolRouter(
            new SemanticToolSelector(
                mock(SemanticReferenceSearchPort.class), new SemanticMatchPolicy(0.90, 0.10)),
            gateway,
            "es-MX");

    assertThat(
            AiExecutionContextScope.call(
                context(),
                () -> router.route(new SemanticQuery("what services do you have?", QUERY))))
        .isEmpty();
    verify(gateway).proactivelyEligibleToolKeys();
  }

  @Test
  void abstainsWhenSemanticSelectionDoesNotMeetTheConfidencePolicy() {
    SemanticReferenceSearchPort search = mock(SemanticReferenceSearchPort.class);
    when(search.searchTools("es-MX", QUERY, Set.of("getSalonServices"), 2))
        .thenReturn(List.of(new SemanticMatch("getSalonServices", 0.91)));
    AiToolGateway gateway = mock(AiToolGateway.class);
    when(gateway.proactivelyEligibleToolKeys()).thenReturn(Set.of("getSalonServices"));
    SemanticProactiveToolRouter router =
        new SemanticProactiveToolRouter(
            new SemanticToolSelector(search, new SemanticMatchPolicy(0.95, 0.10)),
            gateway,
            "es-MX");

    assertThat(
            AiExecutionContextScope.call(
                context(), () -> router.route(new SemanticQuery("maybe", QUERY))))
        .isEmpty();
    verify(gateway).proactivelyEligibleToolKeys();
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("client"),
        id,
        id,
        "trace-" + id,
        "idem-" + id);
  }
}
