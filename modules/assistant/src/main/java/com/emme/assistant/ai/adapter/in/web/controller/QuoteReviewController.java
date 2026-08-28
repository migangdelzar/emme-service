package com.emme.assistant.ai.adapter.in.web.controller;

import com.emme.assistant.ai.adapter.in.web.request.ReviewQuoteRequest;
import com.emme.assistant.ai.adapter.in.web.response.QuoteReviewResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.command.ReviewQuoteCommand;
import com.emme.assistant.ai.api.usecase.ReviewQuoteUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.tracing.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff-only HTTP boundary for resolving a persisted quote review task. */
@RestController
@Validated
@ConditionalOnProperty(prefix = "app.ai.quote", name = "enabled", havingValue = "true")
@RequestMapping(path = "/api/ai/quotes/reviews", version = "1.0")
@Tag(name = "AI Quote Reviews")
public class QuoteReviewController {

  private final ReviewQuoteUseCase reviewQuote;
  private final AiWebExecutionContextFactory contextFactory;

  public QuoteReviewController(
      ReviewQuoteUseCase reviewQuote, AiWebExecutionContextFactory contextFactory) {
    this.reviewQuote = Objects.requireNonNull(reviewQuote, "reviewQuote must not be null");
    this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory must not be null");
  }

  @PostMapping("/{reviewTaskId}")
  @Operation(summary = "Approve, edit, or reject a pending AI quote review")
  @PreAuthorize("hasAnyRole('admin', 'tenant_owner', 'tenant_staff')")
  public ResponseEntity<QuoteReviewResponse> review(
      @PathVariable UUID reviewTaskId,
      @Valid @RequestBody ReviewQuoteRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    Objects.requireNonNull(jwt, "Authenticated JWT is required");
    Objects.requireNonNull(authentication, "Authenticated principal is required");
    String traceId = requireCorrelationId();
    AiExecutionContext context =
        contextFactory.forReview(
            reviewTaskId,
            traceId,
            idempotencyKey,
            Objects.requireNonNull(jwt.getIssuer(), "JWT issuer is required").toString(),
            jwt.getSubject(),
            authentication.getAuthorities());
    var command =
        new ReviewQuoteCommand(
            reviewTaskId, request.expectedVersion(), request.decision(), request.notes());
    var result = AiExecutionContextScope.call(context, () -> reviewQuote.review(command));
    return ResponseEntity.ok(QuoteReviewResponse.from(result));
  }

  private static String requireCorrelationId() {
    String traceId = CorrelationId.get();
    if (traceId == null || traceId.isBlank()) {
      throw new IllegalStateException("Correlation ID is required for AI quote review");
    }
    return traceId;
  }
}
