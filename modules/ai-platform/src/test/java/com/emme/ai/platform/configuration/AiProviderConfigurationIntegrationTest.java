package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.model.AiModelProvider;
import java.util.concurrent.TimeUnit;
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

    AiModelProvider provider =
        configuration.groqModelProvider(configuration.groqChatClient(properties), properties);
    String response = provider.chat("", "hello");

    assertThat(response).isEqualTo("Hola desde Groq");
    assertThat(provider.name()).isEqualTo("groq");
    assertThat(provider.embed("faq")).isEmpty();
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).isEqualTo("/openai/v1/chat/completions");
    assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-groq-key");
    assertThat(request.getBody().readUtf8())
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

  private static MockResponse jsonResponse(String body) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body);
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
}
