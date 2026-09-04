package com.emme.ai.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RagAnswerService;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.ai.contracts.workflow.ConversationWorkflow;
import com.emme.ai.contracts.workflow.QuoteWorkflow;
import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CanonicalAiContractsTest {

  @Test
  void chatCompletionCarriesTrustedContextProviderPolicyAndResultIdentity() {
    var context = executionContext();
    var request =
        new AiChatCompletion.Request(
            "previous turn",
            "new question",
            context,
            new AiChatCompletion.ProviderPolicy(List.of("ollama", "openai"), true));
    AiChatCompletion completion =
        received -> {
          assertThat(received).isSameAs(request);
          return new ChatResponse("answer", "ollama", "qwen3", 12, 4);
        };

    assertThat(completion.complete(request).provider()).isEqualTo("ollama");
  }

  @Test
  void embeddingServiceIsTheSingleVerbOrientedCrossModuleCapability() {
    EmbeddingService embeddings = text -> List.of(text.length() * 1.0f);

    assertThat(embeddings.embed("nails")).containsExactly(5.0f);
  }

  @Test
  void retrievalAndAnswerPolicyAreSeparateCapabilities() {
    var context = executionContext();
    var query = new KnowledgeQuery("aftercare", "en-US", 3);
    var document = new RetrievedDocument("guide-1", "Use cuticle oil.", Map.of(), 0.9);
    KnowledgeRetriever retriever = (receivedQuery, receivedContext) -> List.of(document);
    RagAnswerService answers = (question, receivedContext) -> "Use cuticle oil.";

    assertThat(retriever.search(query, context)).containsExactly(document);
    assertThat(answers.answer(query.text(), context)).isEqualTo(document.content());
  }

  @Test
  void workflowCapabilitiesExposeBusinessCommandsWithoutGraphTypes() {
    var context = executionContext();
    var command = new WorkflowCommand(UUID.randomUUID(), "conversation", Map.of(), "idem-1");
    var handle = new WorkflowHandle(command.workflowId(), WorkflowStatus.RUNNING, 0);
    ConversationWorkflow conversations = (received, receivedContext) -> handle;
    QuoteWorkflow quotes =
        new QuoteWorkflow() {
          @Override
          public WorkflowHandle start(
              WorkflowCommand received, AiExecutionContext receivedContext) {
            return handle;
          }

          @Override
          public WorkflowHandle resume(
              WorkflowCommand received, AiExecutionContext receivedContext) {
            return handle;
          }
        };

    assertThat(conversations.startOrResume(command, context)).isEqualTo(handle);
    assertThat(quotes.start(command, context)).isEqualTo(handle);
    assertThat(quotes.resume(command, context)).isEqualTo(handle);
  }

  @Test
  void contractSourcesContainNoFrameworkDatabaseMessagingOrProviderTypes() throws IOException {
    Path root = sourcePath("libraries/ai-contracts/src/main/java/com/emme/ai/contracts");
    List<String> forbidden =
        List.of(
            "org.springframework",
            "jakarta.persistence",
            "javax.persistence",
            "java.sql",
            "org.bsc.langgraph4j",
            "okhttp3",
            "redis.clients",
            "org.apache.kafka",
            "com.openai",
            "com.google.genai",
            "com.anthropic");

    try (Stream<Path> sources = Files.walk(root)) {
      for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
        assertThat(Files.readString(source))
            .as("framework-neutral AI contract: %s", source)
            .doesNotContain(forbidden.toArray(String[]::new));
      }
    }
  }

  @Test
  void uncalledDuplicateCapabilityDeclarationsAreRemoved() {
    assertThat(
            sourcePath(
                "libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/ChatCompletionPort.java"))
        .doesNotExist();
    assertThat(
            sourcePath(
                "libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/EmbeddingPort.java"))
        .doesNotExist();
    assertThat(
            sourcePath(
                "libraries/ai-contracts/src/main/java/com/emme/ai/contracts/tool/ToolGateway.java"))
        .doesNotExist();
    assertThat(
            sourcePath(
                "libraries/ai-contracts/src/main/java/com/emme/ai/contracts/workflow/WorkflowRuntime.java"))
        .doesNotExist();
    assertThat(
            sourcePath(
                "libraries/ai-contracts/src/main/java/com/emme/ai/contracts/embedding/EmbedTextUseCase.java"))
        .doesNotExist();
  }

  private static AiExecutionContext executionContext() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idem-1");
  }

  private static Path sourcePath(String relativePath) {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) return candidate;
      current = current.getParent();
    }
    return Path.of("").toAbsolutePath().resolve(relativePath);
  }
}
