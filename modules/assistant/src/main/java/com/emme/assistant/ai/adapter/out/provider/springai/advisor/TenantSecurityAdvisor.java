package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/** Adds backend-resolved correlation metadata and rejects model calls without trusted context. */
public final class TenantSecurityAdvisor implements BaseAdvisor {

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    Objects.requireNonNull(request, "request must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return request
        .mutate()
        .context("tenantId", context.tenantId().toString())
        .context("principalId", context.principalId().toString())
        .context("conversationId", context.conversationId().toString())
        .context("workflowId", context.workflowId().toString())
        .context("traceId", context.traceId())
        .build();
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    return response;
  }

  @Override
  public String getName() {
    return "emme-tenant-security";
  }

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE;
  }
}
