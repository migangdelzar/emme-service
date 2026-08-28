package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.Prompt;

class TenantSecurityAdvisorTest {

  @Test
  void addsOnlyBackendResolvedIdentityToTheSpringAiRequestContext() {
    TenantSecurityAdvisor advisor = new TenantSecurityAdvisor();
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_tenant_staff"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-1",
            "idem-1");

    ChatClientRequest request =
        AiExecutionContextScope.call(
            context,
            () -> advisor.before(new ChatClientRequest(new Prompt("hello"), Map.of()), null));

    assertThat(request.context())
        .containsEntry("tenantId", context.tenantId().toString())
        .containsEntry("principalId", context.principalId().toString())
        .containsEntry("conversationId", context.conversationId().toString())
        .containsEntry("workflowId", context.workflowId().toString())
        .containsEntry("traceId", "trace-1");
    assertThat(request.context()).doesNotContainKey("roles");
  }

  @Test
  void failsClosedWhenNoBackendAiContextIsBound() {
    TenantSecurityAdvisor advisor = new TenantSecurityAdvisor();

    assertThatThrownBy(
            () -> advisor.before(new ChatClientRequest(new Prompt("hello"), Map.of()), null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }
}
