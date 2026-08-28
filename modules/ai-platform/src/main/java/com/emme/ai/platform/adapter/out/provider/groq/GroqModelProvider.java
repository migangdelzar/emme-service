package com.emme.ai.platform.adapter.out.provider.groq;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.platform.configuration.AiProviderHttpClient;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
 * Groq model provider via raw HTTP (OkHttp) — OpenAI-compatible API.
 *
 * <p>Activated when app.ai.provider=groq. Falls back to MockModelProvider when Groq is not
 * configured.
 *
 * <p>Uses free-tier Groq API: https://api.groq.com/openai/v1 Model: llama-3.3-70b-versatile Rate
 * limit: ~30 req/min
 *
 * <p>Configure via application.yml: app.ai.provider: groq app.ai.chat.model:
 * llama-3.3-70b-versatile app.ai.chat.base-url: https://api.groq.com/openai/v1 app.ai.chat.api-key:
 * ${GROQ_API_KEY:}
 *
 * <p>The secret is supplied through {@link AiProviderProperties}; the provider does not read
 * process environment variables directly.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
public class GroqModelProvider implements AiModelProvider {

  private static final Logger log = LoggerFactory.getLogger(GroqModelProvider.class);

  private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
  private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
  private static final String SYSTEM_PROMPT =
      "You are EMME, a helpful salon assistant. Be concise and friendly. "
          + "Respond in the same language as the user.";

  private final String apiKey;
  private final String model;
  private final String baseUrl;
  private final AiProviderHttpClient client;
  private final ObjectMapper mapper;

  public GroqModelProvider(
      AiProviderProperties props, AiProviderHttpClient client, ObjectMapper mapper) {
    this.apiKey =
        props.chat() != null && props.chat().apiKey() != null ? props.chat().apiKey() : "";
    this.model =
        props.chat() != null && props.chat().model() != null ? props.chat().model() : DEFAULT_MODEL;
    this.baseUrl =
        props.chat() != null && props.chat().baseUrl() != null
            ? props.chat().baseUrl()
            : DEFAULT_BASE_URL;
    this.client = client;
    this.mapper = mapper;

    log.info(
        "GroqModelProvider initialized — model={}, baseUrl={}, apiKeyPresent={}",
        model,
        baseUrl,
        apiKey != null && !apiKey.isBlank());
  }

  @Override
  public String name() {
    return "groq";
  }

  @Override
  public String chat(String context, String userMessage) {
    if (apiKey == null || apiKey.isBlank()) {
      return "[Groq] API key not configured. Set app.ai.chat.api-key.";
    }

    try {
      String systemContent =
          context.isEmpty() ? SYSTEM_PROMPT : SYSTEM_PROMPT + "\n\nContext:\n" + context;

      Map<String, Object> body =
          Map.of(
              "model",
              model,
              "messages",
              List.of(
                  Map.of("role", "system", "content", systemContent),
                  Map.of("role", "user", "content", userMessage)),
              "temperature",
              0.7,
              "max_tokens",
              1024);

      Request request =
          new Request.Builder()
              .url(baseUrl + "/chat/completions")
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .post(
                  RequestBody.create(
                      mapper.writeValueAsString(body), MediaType.get("application/json")))
              .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "no body";
          log.warn("Groq API error: status={}, body={}", response.code(), errorBody);
          return "[Groq] API error: " + response.code() + " — " + errorBody;
        }

        String responseBody = response.body() != null ? response.body().string() : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        if (choices != null && !choices.isEmpty()) {
          @SuppressWarnings("unchecked")
          Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
          String content = (String) msg.get("content");
          return content != null ? content.strip() : "[Groq] Empty response content";
        }
        return "[Groq] No response content";
      }
    } catch (IOException e) {
      log.error("Groq API call failed", e);
      return "[Groq] Error: " + e.getMessage();
    }
  }

  @Override
  public List<Float> embed(String text) {
    // Groq does not offer embeddings. Do not persist a zero vector: cosine distance is undefined.
    log.debug("Groq does not support embeddings — leaving text unembedded");
    return List.of();
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
          "Groq returned unknown intent '{}' for message '{}', defaulting to GENERAL",
          intent,
          message);
      intent = "GENERAL";
    }

    return new IntentResult(intent, 0.9, Map.of("provider", "groq"));
  }

  @Override
  public boolean isMock() {
    return false;
  }
}
