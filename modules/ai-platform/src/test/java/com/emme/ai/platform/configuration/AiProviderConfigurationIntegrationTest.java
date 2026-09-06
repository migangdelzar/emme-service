package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiProviderConfigurationIntegrationTest {

  private MockWebServer server;

  @BeforeEach
  void startServer() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void stopServer() throws Exception {
    server.shutdown();
  }

  @Test
  void createsAGroqOpenAiCompatibleClientThatUsesConfiguredEndpointAndCredentials()
      throws Exception {
    server.enqueue(
        jsonResponse(
            "{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\","
                + "\"created\":1700000000,\"model\":\"llama-test\","
                + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":\"Hola desde Groq\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":3,"
                + "\"total_tokens\":4}}"));
    AiProviderConfiguration configuration = new AiProviderConfiguration();
    AiProviderProperties properties =
        properties("groq", "llama-test", server.url("/openai/v1").toString(), "test-groq-key");

    AiChatCompletion completion =
        configuration.groqChatCompletion(configuration.groqChatClient(properties), properties);
    AiExecutionContext context = context();
    AiChatCompletion.Request request =
        new AiChatCompletion.Request(
            "", "hello", context, new AiChatCompletion.ProviderPolicy(List.of("groq"), false));
    var completionResponse =
        AiExecutionContextScope.call(context, () -> completion.complete(request));

    assertThat(completionResponse.content()).isEqualTo("Hola desde Groq");
    assertThat(completionResponse.provider()).isEqualTo("groq");
    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context, () -> configuration.unsupportedGroqEmbedding(properties).embed("faq")))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Provider 'groq' does not support embeddings");
    RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS);
    assertThat(recordedRequest).isNotNull();
    assertThat(recordedRequest.getPath()).isEqualTo("/openai/v1/chat/completions");
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-groq-key");
    assertThat(recordedRequest.getBody().readUtf8())
        .contains("\"model\":\"llama-test\"")
        .contains("\"content\":\"hello\"");
  }

  @Test
  void createsAnOllamaClientThatUsesTheConcreteSpringAiChatFactory() throws Exception {
    server.enqueue(
        jsonResponse(
            "{\"model\":\"ollama-test\",\"created_at\":\"2026-09-02T00:00:00Z\","
                + "\"message\":{\"role\":\"assistant\",\"content\":\"Hola local\"},"
                + "\"done\":true}"));
    AiProviderConfiguration configuration = new AiProviderConfiguration();
    AiProviderProperties properties =
        properties("ollama", "ollama-test", server.url("/").toString(), null);

    String response =
        configuration.ollamaChatClient(properties).prompt().user("hello").call().content();

    assertThat(response).isEqualTo("Hola local");
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/api/chat");
    assertThat(request.getBody().readUtf8())
        .contains("\"model\":\"ollama-test\"")
        .contains("\"stream\":false")
        .contains("\"content\":\"hello\"");
  }

  @Test
  void createsAnOllamaEmbeddingModelThatUsesTheConcreteSpringAiEmbeddingFactory() throws Exception {
    server.enqueue(jsonResponse("{\"embeddings\":[[0.25,0.75]]}"));
    AiProviderConfiguration configuration = new AiProviderConfiguration();
    AiProviderProperties properties =
        properties("ollama", "ollama-test", server.url("/").toString(), null);

    float[] embedding = configuration.ollamaEmbeddingModel(properties).embed("faq");

    assertThat(embedding).containsExactly(0.25f, 0.75f);
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/api/embed");
    assertThat(request.getBody().readUtf8())
        .contains("\"model\":\"embeddinggemma:300m\"")
        .contains("\"input\":[\"faq\"]");
  }

  @Test
  void delegatesChatProviderAndModelObservationFieldsToSpring() throws Exception {
    server.enqueue(
        jsonResponse(
            "{\"model\":\"ollama-test\",\"created_at\":\"2026-09-02T00:00:00Z\","
                + "\"message\":{\"role\":\"assistant\",\"content\":\"Hola local\"},"
                + "\"done\":true}"));
    ObservationCapture capture = new ObservationCapture();
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(capture);
    AiProviderConfiguration configuration = new AiProviderConfiguration();
    AiProviderProperties properties =
        properties("ollama", "ollama-test", server.url("/").toString(), null);

    configuration.ollamaChatClient(properties, registry).prompt().user("hello").call().content();

    assertThat(capture.context()).isNotNull();
    assertThat(capture.value("gen_ai.system")).isEqualTo("ollama");
    assertThat(capture.value("gen_ai.request.model")).isEqualTo("ollama-test");
  }

  @Test
  void delegatesEmbeddingProviderAndModelObservationFieldsToSpring() throws Exception {
    server.enqueue(jsonResponse("{\"embeddings\":[[0.25,0.75]]}"));
    ObservationCapture capture = new ObservationCapture();
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(capture);
    AiProviderConfiguration configuration = new AiProviderConfiguration();
    AiProviderProperties properties =
        properties("ollama", "ollama-test", server.url("/").toString(), null);

    configuration.ollamaEmbeddingModel(properties, registry).embed("faq");

    assertThat(capture.context()).isNotNull();
    assertThat(capture.value("gen_ai.system")).isEqualTo("ollama");
    assertThat(capture.value("gen_ai.request.model")).isEqualTo("embeddinggemma:300m");
  }

  private static MockResponse jsonResponse(String body) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body);
  }

  private static final class ObservationCapture implements ObservationHandler<Observation.Context> {
    private final AtomicReference<Observation.Context> context = new AtomicReference<>();

    @Override
    public boolean supportsContext(Observation.Context context) {
      return true;
    }

    @Override
    public void onStop(Observation.Context context) {
      if ("gen_ai.client.operation".equals(context.getName())) {
        this.context.set(context);
      }
    }

    private Observation.Context context() {
      return context.get();
    }

    private String value(String key) {
      var keyValue = context().getLowCardinalityKeyValue(key);
      return keyValue == null ? null : keyValue.getValue();
    }
  }

  private AiProviderProperties properties(
      String provider, String model, String baseUrl, String apiKey) {
    return new AiProviderProperties(
        provider,
        new AiProviderProperties.ProviderConfig(model, baseUrl, apiKey),
        new AiProviderProperties.EmbeddingConfig(
            "embeddinggemma:300m", baseUrl, null, 768, "embeddinggemma-300m-v1"),
        false);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }
}
