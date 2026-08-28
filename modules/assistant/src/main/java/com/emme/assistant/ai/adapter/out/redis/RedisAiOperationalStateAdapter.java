package com.emme.assistant.ai.adapter.out.redis;

import com.emme.assistant.ai.application.port.out.AiOperationalStatePort;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/** Redis adapter for temporary workflow state and tenant/conversation locks. */
public final class RedisAiOperationalStateAdapter implements AiOperationalStatePort {

  private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
      new DefaultRedisScript<>(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
          end
          return 0
          """,
          Long.class);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public RedisAiOperationalStateAdapter(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = Objects.requireNonNull(redis, "redis must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void save(WorkflowState state, Duration ttl) {
    Objects.requireNonNull(state, "state must not be null");
    requirePositive(ttl, "ttl");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    redis
        .opsForValue()
        .set(
            RedisAiKeys.workflowStateKey(context.tenantId(), context.workflowId()),
            serialize(state),
            ttl);
  }

  @Override
  public Optional<WorkflowState> load() {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    String payload =
        redis
            .opsForValue()
            .get(RedisAiKeys.workflowStateKey(context.tenantId(), context.workflowId()));
    return payload == null ? Optional.empty() : Optional.of(deserialize(payload));
  }

  @Override
  public boolean tryAcquireConversationLock(Duration lease) {
    requirePositive(lease, "lease");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    Boolean acquired =
        redis
            .opsForValue()
            .setIfAbsent(
                RedisAiKeys.conversationLockKey(context.tenantId(), context.conversationId()),
                context.idempotencyKey(),
                lease);
    return Boolean.TRUE.equals(acquired);
  }

  @Override
  public boolean releaseConversationLock() {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    Long released =
        redis.execute(
            RELEASE_LOCK_SCRIPT,
            List.of(RedisAiKeys.conversationLockKey(context.tenantId(), context.conversationId())),
            context.idempotencyKey());
    return released != null && released == 1L;
  }

  private String serialize(WorkflowState state) {
    try {
      return objectMapper.writeValueAsString(state);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to serialize AI operational state", exception);
    }
  }

  private WorkflowState deserialize(String payload) {
    try {
      return objectMapper.readValue(payload, WorkflowState.class);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to deserialize AI operational state", exception);
    }
  }

  private static void requirePositive(Duration duration, String field) {
    Objects.requireNonNull(duration, field + " must not be null");
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
  }
}
