package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;

/**
 * Protects a LangGraph checkpoint adapter with the backend-resolved AI context.
 *
 * <p>LangGraph's thread ID is treated as the workflow ID. The delegate remains replaceable so tests
 * can use {@code MemorySaver}, while production wiring can use a PostgreSQL adapter that persists
 * Emme's tenant-scoped checkpoint record.
 */
public final class TenantAwareCheckpointSaver implements BaseCheckpointSaver {

  private final BaseCheckpointSaver delegate;

  public TenantAwareCheckpointSaver(BaseCheckpointSaver delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
  }

  @Override
  public Collection<Checkpoint> list(RunnableConfig config) {
    return delegate.list(validateConfig(config));
  }

  @Override
  public Optional<Checkpoint> get(RunnableConfig config) {
    return delegate.get(validateConfig(config));
  }

  @Override
  public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
    Objects.requireNonNull(checkpoint, "checkpoint must not be null");
    return delegate.put(validateConfig(config), checkpoint);
  }

  @Override
  public Tag release(RunnableConfig config) throws Exception {
    return delegate.release(validateConfig(config));
  }

  static RunnableConfig validateConfig(RunnableConfig config) {
    Objects.requireNonNull(config, "config must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    String expectedThreadId = context.workflowId().toString();
    if (config
        .threadId()
        .filter(
            threadId ->
                threadId.equals(expectedThreadId) || threadId.startsWith(expectedThreadId + ":"))
        .isEmpty()) {
      throw new IllegalArgumentException("Checkpoint thread does not match AI workflow context");
    }
    return config;
  }
}
