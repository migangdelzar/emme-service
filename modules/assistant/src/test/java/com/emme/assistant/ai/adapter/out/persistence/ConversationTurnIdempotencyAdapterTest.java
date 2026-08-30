package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationTurnIdempotencyAdapterTest {

  @Test
  void storesAndReplaysConversationResultsThroughTheExistingDurableStore() {
    InMemoryAiToolIdempotencyStore store = new InMemoryAiToolIdempotencyStore();
    ConversationTurnIdempotencyAdapter adapter =
        new ConversationTurnIdempotencyAdapter(store, new ObjectMapper());
    UUID conversationId = UUID.randomUUID();
    ProcessConversationResult result =
        new ProcessConversationResult(conversationId, UUID.randomUUID(), "answer");

    ProcessConversationResult replay =
        AiExecutionContextScope.call(
                context(conversationId),
                () -> {
                  assertThat(adapter.reserve(conversationId, "turn-1")).isTrue();
                  adapter.complete(conversationId, "turn-1", result);
                  return adapter.find(conversationId, "turn-1");
                })
            .orElseThrow();

    assertThat(replay).isEqualTo(result);
    assertThat(store.claimedOperationKey)
        .isEqualTo("processConversation:" + conversationId + ":turn-1");
    assertThat(store.claimedToolKey).isEqualTo("processConversation");
  }

  private static AiExecutionContext context(UUID conversationId) {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        conversationId,
        UUID.randomUUID(),
        "trace-conversation-idempotency",
        "turn-1");
  }

  private static final class InMemoryAiToolIdempotencyStore implements AiToolIdempotencyStore {

    private final Map<String, AiToolResult> completed = new HashMap<>();
    private String claimedOperationKey;
    private String claimedToolKey;

    @Override
    public Optional<AiToolResult> find(String operationKey) {
      return Optional.ofNullable(completed.get(operationKey));
    }

    @Override
    public boolean claim(String operationKey, String toolKey) {
      claimedOperationKey = operationKey;
      claimedToolKey = toolKey;
      return !completed.containsKey(operationKey);
    }

    @Override
    public void complete(String operationKey, AiToolResult result) {
      completed.put(operationKey, result);
    }

    @Override
    public void release(String operationKey) {
      completed.remove(operationKey);
    }
  }
}
