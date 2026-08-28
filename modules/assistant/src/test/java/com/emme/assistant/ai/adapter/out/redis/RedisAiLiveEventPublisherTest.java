package com.emme.assistant.ai.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.AiLiveEventPublisher;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisAiLiveEventPublisherTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void publishesOnlySafeStatusFieldsToTheTenantConversationStream() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    StreamOperations<String, Object, Object> streams = mock(StreamOperations.class);
    when(redis.opsForStream()).thenReturn(streams);
    AiLiveEventPublisher publisher = new RedisAiLiveEventPublisher(redis);
    AiLiveEventPublisher.LiveEvent event =
        new AiLiveEventPublisher.LiveEvent(
            "WORKFLOW_STATUS",
            "WAITING_FOR_STAFF",
            "Waiting for nail artist",
            Instant.parse("2026-08-28T00:00:00Z"));

    AiExecutionContextScope.run(context(), () -> publisher.publish(event));

    ArgumentCaptor<Map<String, String>> fields = ArgumentCaptor.forClass(Map.class);
    verify(streams)
        .add(eq(RedisAiKeys.liveEventStreamKey(TENANT_ID, CONVERSATION_ID)), fields.capture());
    assertThat(fields.getValue())
        .containsEntry("type", "WORKFLOW_STATUS")
        .containsEntry("status", "WAITING_FOR_STAFF")
        .containsEntry("message", "Waiting for nail artist")
        .containsEntry("occurredAt", "2026-08-28T00:00:00Z")
        .doesNotContainKey("token");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("tenant_client"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "request-1");
  }
}
