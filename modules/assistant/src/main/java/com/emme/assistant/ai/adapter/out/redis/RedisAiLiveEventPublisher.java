package com.emme.assistant.ai.adapter.out.redis;

import com.emme.assistant.ai.application.port.out.AiLiveEventPublisher;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis Streams adapter for safe, reconnectable AI status events. */
public final class RedisAiLiveEventPublisher implements AiLiveEventPublisher {

  private final StringRedisTemplate redis;

  public RedisAiLiveEventPublisher(StringRedisTemplate redis) {
    this.redis = Objects.requireNonNull(redis, "redis must not be null");
  }

  @Override
  public void publish(LiveEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("type", event.type());
    fields.put("status", event.status());
    fields.put("message", event.message());
    fields.put("occurredAt", event.occurredAt().toString());
    redis
        .opsForStream()
        .add(RedisAiKeys.liveEventStreamKey(context.tenantId(), context.conversationId()), fields);
  }
}
