package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

class TenantAwareCheckpointSaverTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void delegatesCheckpointOperationsForTheAuthenticatedWorkflow() throws Exception {
    TenantAwareCheckpointSaver saver = new TenantAwareCheckpointSaver(new MemorySaver());
    RunnableConfig config = configFor(WORKFLOW_ID);
    Checkpoint checkpoint = checkpoint("checkpoint-1");

    RunnableConfig returned = runWithContext(() -> saver.put(config, checkpoint));

    assertThat(returned.threadId()).contains(WORKFLOW_ID.toString());
    assertThat(runWithContext(() -> saver.get(config)).orElseThrow().getId())
        .isEqualTo(checkpoint.getId());
    assertThat(runWithContext(() -> saver.list(config)))
        .extracting(Checkpoint::getId)
        .containsExactly(checkpoint.getId());
  }

  @Test
  void rejectsCheckpointAccessWhenTheWorkflowDoesNotMatchTheBackendContext() {
    TenantAwareCheckpointSaver saver = new TenantAwareCheckpointSaver(new MemorySaver());
    RunnableConfig config = configFor(UUID.randomUUID());

    assertThatThrownBy(() -> runWithContext(() -> saver.get(config)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Checkpoint thread does not match AI workflow context");
  }

  @Test
  void rejectsCheckpointAccessWithoutAnAuthenticatedAiContext() {
    TenantAwareCheckpointSaver saver = new TenantAwareCheckpointSaver(new MemorySaver());

    assertThatThrownBy(() -> saver.get(configFor(WORKFLOW_ID)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void rejectsCheckpointAccessWithoutAWorkflowThreadId() {
    TenantAwareCheckpointSaver saver = new TenantAwareCheckpointSaver(new MemorySaver());

    assertThatThrownBy(() -> runWithContext(() -> saver.get(RunnableConfig.empty())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Checkpoint thread does not match AI workflow context");
  }

  @Test
  void rejectsMalformedCheckpointNamespacesBeforeCallingTheProvider() {
    MemorySaver delegate = new MemorySaver();
    TenantAwareCheckpointSaver saver = new TenantAwareCheckpointSaver(delegate);
    RunnableConfig malformedConfig =
        RunnableConfig.builder().threadId(WORKFLOW_ID + ":invalid:namespace").build();

    assertThatThrownBy(() -> runWithContext(() -> saver.get(malformedConfig)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Checkpoint thread namespace must not contain ':'");
  }

  private static RunnableConfig configFor(UUID workflowId) {
    return RunnableConfig.builder().threadId(workflowId.toString()).build();
  }

  private static Checkpoint checkpoint(String id) {
    return Checkpoint.builder()
        .id(id)
        .state(java.util.Map.of("status", "RECEIVED"))
        .nodeId("receive_request")
        .nextNodeId("normalize_input")
        .build();
  }

  private static <T> T runWithContext(CheckedSupplier<T> action) {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-1",
            "idempotency-1");
    return AiExecutionContextScope.call(context, action::get);
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
