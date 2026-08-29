package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RedisSemanticCacheHotStoreTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final EmbeddingVector QUERY =
      new EmbeddingVector("embeddinggemma-v1", List.of(1.0f, 0.0f));

  @Test
  void readsAHotCandidateUsingTheQueryAndTheBackendTenantFilter() {
    VectorStore vectorStore = mock(VectorStore.class);
    UUID durableId = UUID.randomUUID();
    Document document =
        Document.builder()
            .id("hot-entry")
            .text("What are your hours?")
            .metadata(
                Map.of(
                    "durableCacheId", durableId.toString(),
                    "responsePayload", "{\"text\":\"We are open.\"}",
                    "expiresAt", Instant.now().plusSeconds(60).getEpochSecond()))
            .score(0.98)
            .build();
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(vectorStore, "embeddinggemma-v1", 2);

    List<SemanticCachePort.Candidate> result =
        AiExecutionContextScope.call(
            context(),
            () ->
                hotStore.find(
                    new SemanticCachePort.Lookup("CHAT_INFORMATIONAL", "ctx", "chat-v1", QUERY),
                    "What are your hours?",
                    2));

    assertThat(result)
        .singleElement()
        .satisfies(
            candidate -> {
              assertThat(candidate.id()).isEqualTo(durableId);
              assertThat(candidate.responsePayload()).contains("We are open");
              assertThat(candidate.similarity()).isEqualTo(0.98);
            });
    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void projectsDurableEntryWithBackendScopeMetadata() {
    VectorStore vectorStore = mock(VectorStore.class);
    UUID durableId = UUID.randomUUID();
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(vectorStore, "embeddinggemma-v1", 2);
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "CHAT_INFORMATIONAL",
            "What are your hours?",
            "ctx",
            "chat-v1",
            "{\"text\":\"We are open.\"}",
            Instant.now().plusSeconds(60),
            QUERY,
            "write-key");

    AiExecutionContextScope.run(context(), () -> hotStore.put(durableId, write));

    AtomicReference<Document> documentReference = new AtomicReference<>();
    verify(vectorStore)
        .add(
            argThat(
                documents -> {
                  documentReference.set(documents.getFirst());
                  return true;
                }));
    Document document = documentReference.get();
    assertThat(document.getText()).isEqualTo(write.queryText());
    assertThat(document.getMetadata())
        .containsEntry("tenantId", TENANT_ID.toString())
        .containsEntry("principalId", PRINCIPAL_ID.toString())
        .containsEntry("durableCacheId", durableId.toString());
  }

  @Test
  void rejectsHotOperationsWithoutTheBackendContext() {
    RedisSemanticCacheHotStore hotStore =
        new RedisSemanticCacheHotStore(mock(VectorStore.class), "embeddinggemma-v1", 2);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                hotStore.find(
                    new SemanticCachePort.Lookup("CHAT_INFORMATIONAL", "ctx", "chat-v1", QUERY),
                    "hours",
                    1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-hot-cache",
        "idempotency-hot-cache");
  }
}
