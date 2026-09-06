package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.assistant.ai.application.guardrail.GroundingGuard;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

/** Enforces source provenance after Spring AI retrieval has populated the request context. */
public final class GroundingGuardAdvisor implements BaseAdvisor {

  private final GroundingGuard guard;

  public GroundingGuardAdvisor(GroundingGuard guard) {
    this.guard = Objects.requireNonNull(guard, "guard must not be null");
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    Objects.requireNonNull(request, "request must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    List<Document> documents =
        documents(request.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT));
    List<Double> scores =
        documents.stream()
            .map(GroundingGuardAdvisor::score)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::orElseThrow)
            .sorted(Comparator.reverseOrder())
            .toList();
    double topScore = scores.isEmpty() ? 0.0 : scores.get(0);
    double margin = scores.size() < 2 ? topScore : topScore - scores.get(1);
    List<String> sourceIds =
        documents.stream().map(Document::getId).filter(id -> id != null && !id.isBlank()).toList();
    GuardrailDecision decision =
        guard.check(
            new GroundingRequest(!documents.isEmpty(), topScore, margin, sourceIds), context);
    if (decision.action() != GuardrailAction.ALLOW) {
      throw new GuardrailRejectedException(decision);
    }
    return request;
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    return response;
  }

  @Override
  public String getName() {
    return "emme-grounding-guard";
  }

  @Override
  public int getOrder() {
    return 50;
  }

  private static List<Document> documents(Object value) {
    if (!(value instanceof List<?> values)) {
      return List.of();
    }
    return values.stream().filter(Document.class::isInstance).map(Document.class::cast).toList();
  }

  private static java.util.Optional<Double> score(Document document) {
    Object value = document.getMetadata().get("score");
    if (!(value instanceof Number number)) {
      return java.util.Optional.empty();
    }
    double score = number.doubleValue();
    return Double.isFinite(score) && score >= 0
        ? java.util.Optional.of(score)
        : java.util.Optional.empty();
  }
}
