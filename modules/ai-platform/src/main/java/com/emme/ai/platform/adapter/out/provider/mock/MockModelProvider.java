package com.emme.ai.platform.adapter.out.provider.mock;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.platform.configuration.AiProviderProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Always-available mock provider. Keyword-based intent routing, echo chat. Used when no real
 * provider (Ollama/OpenAI/Groq) is configured or available. Activated when app.ai.provider is
 * "mock" or not set at all.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockModelProvider implements AiModelProvider {

  private final AiProviderProperties props;

  public MockModelProvider(AiProviderProperties props) {
    this.props = props;
  }

  @Override
  public String name() {
    return "mock";
  }

  @Override
  public String chat(String context, String userMessage) {
    return "MOCK: I received your message: \""
        + userMessage
        + "\". "
        + "Configure a real AI provider (Ollama/OpenAI) for intelligent responses.";
  }

  /**
   * Deterministic bag-of-words embedding: each lowercase token increments a hashed bucket, then the
   * vector is L2-normalized. Texts sharing tokens get higher cosine similarity, so hybrid-search
   * tests behave meaningfully.
   */
  @Override
  public List<Float> embed(String text) {
    int dim = props.embeddingDimension();
    float[] v = new float[dim];
    for (String token : text.toLowerCase(Locale.ROOT).split("\\W+")) {
      if (!token.isBlank()) v[Math.floorMod(token.hashCode(), dim)] += 1.0f;
    }
    double norm = Math.sqrt(IntStream.range(0, dim).mapToDouble(i -> (double) v[i] * v[i]).sum());
    List<Float> out = new ArrayList<>(dim);
    for (float x : v) out.add(norm == 0 ? 0.0f : (float) (x / norm));
    return out;
  }

  @Override
  public IntentResult routeIntent(String message) {
    String lower = message.toLowerCase(Locale.ROOT);
    if (lower.contains("book") || lower.contains("schedule") || lower.contains("reserve"))
      return new IntentResult("BOOK", 0.95, Map.of("action", "schedule_appointment"));
    if (lower.contains("cancel") || lower.contains("delete appointment"))
      return new IntentResult("CANCEL", 0.95, Map.of("action", "cancel_appointment"));
    if (lower.contains("price") || lower.contains("cost") || lower.contains("how much"))
      return new IntentResult("ASK_PRICE", 0.90, Map.of("action", "check_price"));
    if (lower.contains("policy") || lower.contains("refund") || lower.contains("rules"))
      return new IntentResult("ASK_POLICY", 0.85, Map.of("action", "check_policy"));
    return new IntentResult("GENERAL", 0.70, Map.of());
  }

  @Override
  public boolean isMock() {
    return true;
  }
}
