package com.emme.ai.platform.adapter.out.provider.ollama;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.platform.configuration.AiProviderHttpClient;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ollama model provider via raw HTTP (OkHttp).
 *
 * <p>Activated when app.ai.provider=ollama. Falls back to MockModelProvider when Ollama is not
 * running.
 *
 * <p>Ollama REST API: POST /api/chat — chat completion POST /api/embeddings — embedding generation
 *
 * <p>Model: gemma4:e4b-mlx (chat), embeddinggemma:300m (embeddings)
 *
 * <p>Configure via application.yml: app.ai.provider: ollama app.ai.chat.model: gemma4:e4b-mlx
 * app.ai.chat.base-url: http://localhost:11434 app.ai.embedding.model: embeddinggemma:300m
 * app.ai.embedding.base-url: http://localhost:11434
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
public class OllamaModelProvider implements AiModelProvider {

  private static final Logger log = LoggerFactory.getLogger(OllamaModelProvider.class);

  private static final String DEFAULT_CHAT_MODEL = "gemma4:e4b-mlx";
  private static final String DEFAULT_EMBED_MODEL = "embeddinggemma:300m";
  private static final String DEFAULT_BASE_URL = "http://localhost:11434";
  private static final String SYSTEM_PROMPT =
      "You are EMME, a helpful salon assistant. Be concise and friendly. "
          + "Respond in the same language as the user.";

  private final String baseUrl;
  private final String chatModel;
  private final String embedModel;
  private final AiProviderHttpClient client;
  private final ObjectMapper mapper;

  public OllamaModelProvider(
      AiProviderProperties props, AiProviderHttpClient client, ObjectMapper mapper) {
    this.baseUrl =
        stripTrailingSlash(
            props.chat() != null && props.chat().baseUrl() != null
                ? props.chat().baseUrl()
                : DEFAULT_BASE_URL);
    this.chatModel =
        props.chat() != null && props.chat().model() != null
            ? props.chat().model()
            : DEFAULT_CHAT_MODEL;
    this.embedModel =
        props.embedding() != null && props.embedding().model() != null
            ? props.embedding().model()
            : DEFAULT_EMBED_MODEL;
    this.client = client;
    this.mapper = mapper;

    log.info(
        "OllamaModelProvider initialized — chatModel={}, embedModel={}, baseUrl={}",
        chatModel,
        embedModel,
        baseUrl);
  }

  @Override
  public String name() {
    return "ollama";
  }

  @Override
  public String chat(String context, String userMessage) {
    try {
      String systemContent =
          context.isEmpty() ? SYSTEM_PROMPT : SYSTEM_PROMPT + "\n\nContext:\n" + context;

      List<Map<String, String>> messages = new ArrayList<>();
      messages.add(Map.of("role", "system", "content", systemContent));
      if (!context.isEmpty()) {
        messages.add(Map.of("role", "user", "content", context));
      }
      messages.add(Map.of("role", "user", "content", userMessage));

      Map<String, Object> body =
          Map.of(
              "model", chatModel,
              "messages", messages,
              "stream", false);

      Request request =
          new Request.Builder()
              .url(baseUrl + "/api/chat")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "no body";
          log.warn("Ollama API error: status={}, body={}", response.code(), errorBody);
          return "[Ollama] API error: " + response.code() + " — " + errorBody;
        }

        String responseBody = response.body() != null ? response.body().string() : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) result.get("message");
        if (message != null) {
          String content = (String) message.get("content");
          return content != null ? content.strip() : "[Ollama] Empty response content";
        }
        return "[Ollama] No response";
      }
    } catch (IOException e) {
      log.error("Ollama API call failed", e);
      return "[Ollama] Error: " + e.getMessage();
    }
  }

  @Override
  public List<Float> embed(String text) {
    try {
      Map<String, Object> body =
          Map.of(
              "model", embedModel,
              "prompt", text);

      Request request =
          new Request.Builder()
              .url(baseUrl + "/api/embeddings")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.warn("Ollama embedding error: status={}", response.code());
          return List.of();
        }

        String responseBody = response.body() != null ? response.body().string() : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.get("embedding");
        if (embedding == null) {
          return List.of();
        }
        return embedding.stream().map(Double::floatValue).toList();
      }
    } catch (IOException e) {
      log.error("Ollama embedding call failed", e);
      return List.of();
    }
  }

  @Override
  public IntentResult routeIntent(String message) {
    String prompt =
        "Classify this message into ONE intent: BOOK, CANCEL, "
            + "ASK_PRICE, ASK_POLICY, or GENERAL. "
            + "Respond with only the intent name, nothing else.\n\n"
            + "Message: "
            + message;

    String intent = chat("", prompt).trim().toUpperCase();

    // Sanitize: strip any prefix/suffix artifacts from the LLM response
    intent = intent.replaceAll("^[^A-Z_]+", "").replaceAll("[^A-Z_]+$", "");

    // Validate the result is a known intent
    Set<String> valid = Set.of("BOOK", "CANCEL", "ASK_PRICE", "ASK_POLICY", "GENERAL");
    if (!valid.contains(intent)) {
      log.warn(
          "Ollama returned unknown intent '{}' for message '{}', defaulting to GENERAL",
          intent,
          message);
      intent = "GENERAL";
    }

    return new IntentResult(intent, 0.85, Map.of("provider", "ollama"));
  }

  @Override
  public boolean isMock() {
    return false;
  }

  private static String stripTrailingSlash(String url) {
    return url != null ? url.replaceAll("/+$", "") : "";
  }
}
