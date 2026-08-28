package com.emme.assistant.ai.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.AiOperationalStatePort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisAiOperationalStateAdapterTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void storesAndLoadsWorkflowStateUnderTheTenantScopedWorkflowKey() throws Exception {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    AiOperationalStatePort adapter =
        new RedisAiOperationalStateAdapter(redis, new ObjectMapper().findAndRegisterModules());
    AiOperationalStatePort.WorkflowState state =
        new AiOperationalStatePort.WorkflowState(
            "WAITING_FOR_STAFF",
            "Waiting for nail artist",
            Instant.parse("2026-08-28T00:00:00Z"),
            3);
    String key = RedisAiKeys.workflowStateKey(TENANT_ID, WORKFLOW_ID);

    AiExecutionContextScope.run(context(), () -> adapter.save(state, Duration.ofMinutes(5)));
    when(values.get(key))
        .thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(state));

    var loaded = AiExecutionContextScope.call(context(), adapter::load);

    assertThat(loaded).contains(state);
    verify(values).set(eq(key), any(String.class), eq(Duration.ofMinutes(5)));
  }

  @Test
  void acquiresAndReleasesOnlyTheCurrentConversationLock() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(any(String.class), eq("request-1"), eq(Duration.ofSeconds(30))))
        .thenReturn(true);
    when(redis.execute(anyScript(), any(List.class), eq("request-1"))).thenReturn(1L);
    AiOperationalStatePort adapter =
        new RedisAiOperationalStateAdapter(redis, new ObjectMapper().findAndRegisterModules());

    boolean acquired =
        AiExecutionContextScope.call(
            context(), () -> adapter.tryAcquireConversationLock(Duration.ofSeconds(30)));
    boolean released =
        AiExecutionContextScope.call(context(), () -> adapter.releaseConversationLock());

    assertThat(acquired).isTrue();
    assertThat(released).isTrue();
    verify(values)
        .setIfAbsent(
            RedisAiKeys.conversationLockKey(TENANT_ID, CONVERSATION_ID),
            "request-1",
            Duration.ofSeconds(30));
  }

  @Test
  void rejectsNonPositiveStateTtl() {
    AiOperationalStatePort adapter =
        new RedisAiOperationalStateAdapter(
            mock(StringRedisTemplate.class), new ObjectMapper().findAndRegisterModules());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                AiExecutionContextScope.run(
                    context(),
                    () ->
                        adapter.save(
                            new AiOperationalStatePort.WorkflowState(
                                "RUNNING", "", Instant.now(), 1),
                            Duration.ZERO)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @SuppressWarnings("unchecked")
  private static RedisScript<Long> anyScript() {
    return (RedisScript<Long>) any(RedisScript.class);
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
