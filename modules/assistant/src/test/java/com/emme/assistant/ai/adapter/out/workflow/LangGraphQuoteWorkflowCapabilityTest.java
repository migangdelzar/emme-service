package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

class LangGraphQuoteWorkflowCapabilityTest {

  @Test
  void invokesTheExistingCompiledQuoteWorkflowInsteadOfANoOp() throws Exception {
    UUID workflowId = UUID.randomUUID();
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            UUID.randomUUID(),
            workflowId,
            "trace-quote-capability",
            "quote-capability-idempotency");
    LangGraphQuoteWorkflowCapability capability =
        new LangGraphQuoteWorkflowCapability(
            new QuoteWorkflowGraph(new TenantAwareCheckpointSaver(new MemorySaver())).compile());

    var result =
        AiExecutionContextScope.call(
            context,
            () ->
                capability.execute(
                    new ConversationWorkflowCapabilities.WorkflowRequest(
                        "quote this design", context, Map.of())));

    assertThat(result.updates()).containsEntry("quoteWorkflowStatus", "QUOTE_READY");
    assertThat(result.needsApproval()).isFalse();
  }
}
