package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.in.web.request.ReviewQuoteRequest;
import com.emme.assistant.ai.adapter.in.web.response.QuoteReviewResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.command.ReviewQuoteCommand;
import com.emme.assistant.ai.api.result.ReviewQuoteResult;
import com.emme.assistant.ai.api.usecase.ReviewQuoteUseCase;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class QuoteReviewControllerTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID REVIEW_TASK_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID CLIENT_ID = UUID.randomUUID();

  @AfterEach
  void clearRequestContext() {
    CorrelationId.clear();
  }

  @Test
  void createsAReviewCommandFromTrustedJwtAndBackendTenantContext() {
    ReviewQuoteUseCase useCase = mock(ReviewQuoteUseCase.class);
    ReviewQuoteResult result = approvedResult();
    when(useCase.review(any(ReviewQuoteCommand.class))).thenReturn(result);
    QuoteReviewController controller =
        new QuoteReviewController(useCase, new AiWebExecutionContextFactory());
    Jwt jwt =
        Jwt.withTokenValue("token")
            .issuer("https://issuer.example/realms/emme")
            .subject("auth0|staff-123")
            .header("alg", "none")
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_staff")));
    CorrelationId.set("trace-123");

    ResponseEntity<QuoteReviewResponse> response =
        TenantContextHolder.withTenantOverride(
            TENANT_ID,
            () ->
                controller.review(
                    REVIEW_TASK_ID,
                    new ReviewQuoteRequest(0L, QuoteReviewDecisionType.APPROVED, "Looks correct"),
                    "review-idem-1",
                    jwt,
                    authentication));

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().reviewTaskId()).isEqualTo(REVIEW_TASK_ID);
    assertThat(response.getBody().workflowId()).isEqualTo(WORKFLOW_ID);
    verify(useCase)
        .review(
            new ReviewQuoteCommand(
                REVIEW_TASK_ID, 0, QuoteReviewDecisionType.APPROVED, "Looks correct"));
  }

  @Test
  void doesNotAcceptTenantIdFromTheRequestBoundary() {
    ReviewQuoteUseCase useCase = mock(ReviewQuoteUseCase.class);
    when(useCase.review(any(ReviewQuoteCommand.class))).thenReturn(approvedResult());
    QuoteReviewController controller =
        new QuoteReviewController(useCase, new AiWebExecutionContextFactory());
    Jwt jwt =
        Jwt.withTokenValue("token")
            .issuer("https://issuer.example/realms/emme")
            .subject("auth0|staff-123")
            .header("alg", "none")
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_staff")));
    CorrelationId.set("trace-123");

    ResponseEntity<QuoteReviewResponse> response =
        TenantContextHolder.withTenantOverride(
            TENANT_ID,
            () ->
                controller.review(
                    REVIEW_TASK_ID,
                    new ReviewQuoteRequest(0L, QuoteReviewDecisionType.APPROVED, null),
                    "review-idem-2",
                    jwt,
                    authentication));

    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  void failsClosedWhenTheRequestHasNoCorrelationId() {
    ReviewQuoteUseCase useCase = mock(ReviewQuoteUseCase.class);
    QuoteReviewController controller =
        new QuoteReviewController(useCase, new AiWebExecutionContextFactory());
    Jwt jwt =
        Jwt.withTokenValue("token")
            .issuer("https://issuer.example/realms/emme")
            .subject("auth0|staff-123")
            .header("alg", "none")
            .build();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_staff")));

    assertThatThrownBy(
            () ->
                TenantContextHolder.withTenantOverride(
                    TENANT_ID,
                    () ->
                        controller.review(
                            REVIEW_TASK_ID,
                            new ReviewQuoteRequest(0L, QuoteReviewDecisionType.APPROVED, null),
                            "review-idem-3",
                            jwt,
                            authentication)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Correlation ID is required for AI quote review");
  }

  private static ReviewQuoteResult approvedResult() {
    QuoteReviewTask task =
        new QuoteReviewTask(
            REVIEW_TASK_ID,
            TENANT_ID,
            WORKFLOW_ID,
            com.emme.assistant.ai.domain.workflow.QuoteReviewStatus.APPROVED,
            UUID.randomUUID(),
            java.util.Optional.of(QuoteReviewDecisionType.APPROVED),
            null,
            List.of(),
            1);
    QuoteWorkflow workflow =
        new QuoteWorkflow(
            WORKFLOW_ID,
            TENANT_ID,
            CLIENT_ID,
            CONVERSATION_ID,
            QuoteWorkflowState.STAFF_APPROVED,
            "workflow-idem",
            1);
    return new ReviewQuoteResult(task, workflow);
  }
}
