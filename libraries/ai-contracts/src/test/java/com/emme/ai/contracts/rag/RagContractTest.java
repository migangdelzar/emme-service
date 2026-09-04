package com.emme.ai.contracts.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.AiExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RagContractTest {

  @Test
  void knowledgeRetrieverAcceptsQueryAndExecutionContextAndReturnsRetrievedDocuments() {
    var query = new KnowledgeQuery("nail care", "en-US", 5);
    var context = executionContext();
    var document = new RetrievedDocument("source-1", "Use cuticle oil.", Map.of(), 0.91);
    KnowledgeRetriever retriever =
        new KnowledgeRetriever() {
          @Override
          public List<RetrievedDocument> search(
              KnowledgeQuery receivedQuery, AiExecutionContext receivedContext) {
            assertThat(receivedQuery).isSameAs(query);
            assertThat(receivedContext).isSameAs(context);
            return List.of(document);
          }
        };

    assertThat(retriever.search(query, context)).containsExactly(document);
  }

  private static AiExecutionContext executionContext() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
