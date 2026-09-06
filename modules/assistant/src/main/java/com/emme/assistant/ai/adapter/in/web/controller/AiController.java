package com.emme.assistant.ai.adapter.in.web.controller;

import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.assistant.ai.adapter.in.web.request.ChatRequest;
import com.emme.assistant.ai.adapter.in.web.request.IntentRequest;
import com.emme.assistant.ai.adapter.in.web.request.RagRequest;
import com.emme.assistant.ai.adapter.in.web.response.ChatResponse;
import com.emme.assistant.ai.adapter.in.web.response.RagResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.api.usecase.ProcessConversationUseCase;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.guardrail.DeliveryGuard;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.tracing.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai", version = "1.0")
@Tag(name = "AI")
public class AiController {

  private static final int UNBOUNDED_WEB_RESPONSE_CHARACTERS = Integer.MAX_VALUE;

  private final ChatUseCase chatUseCase;
  private final DetectIntentUseCase detectIntent;
  private final RagQueryUseCase ragQuery;
  private final Optional<ProcessConversationUseCase> processConversation;
  private final AiWebExecutionContextFactory contextFactory;
  private final Optional<DeliveryGuard> deliveryGuard;

  /** Constructor used by Spring and by callers that support durable conversations. */
  public AiController(
      ChatUseCase chatUseCase,
      DetectIntentUseCase detectIntent,
      RagQueryUseCase ragQuery,
      ProcessConversationUseCase processConversation,
      AiWebExecutionContextFactory contextFactory) {
    this(
        chatUseCase, detectIntent, ragQuery, processConversation, contextFactory, Optional.empty());
  }

  @Autowired
  public AiController(
      ChatUseCase chatUseCase,
      DetectIntentUseCase detectIntent,
      RagQueryUseCase ragQuery,
      ProcessConversationUseCase processConversation,
      AiWebExecutionContextFactory contextFactory,
      Optional<DeliveryGuard> deliveryGuard) {
    this(
        chatUseCase,
        detectIntent,
        ragQuery,
        Optional.of(
            Objects.requireNonNull(processConversation, "processConversation must not be null")),
        contextFactory,
        deliveryGuard);
  }

  /** Backwards-compatible constructor for clients that only use the legacy chat endpoint. */
  public AiController(
      ChatUseCase chatUseCase,
      DetectIntentUseCase detectIntent,
      RagQueryUseCase ragQuery,
      AiWebExecutionContextFactory contextFactory) {
    this(chatUseCase, detectIntent, ragQuery, Optional.empty(), contextFactory, Optional.empty());
  }

  private AiController(
      ChatUseCase chatUseCase,
      DetectIntentUseCase detectIntent,
      RagQueryUseCase ragQuery,
      Optional<ProcessConversationUseCase> processConversation,
      AiWebExecutionContextFactory contextFactory,
      Optional<DeliveryGuard> deliveryGuard) {
    this.chatUseCase = chatUseCase;
    this.detectIntent = detectIntent;
    this.ragQuery = ragQuery;
    this.processConversation =
        Objects.requireNonNull(processConversation, "processConversation must not be null");
    this.contextFactory = contextFactory;
    this.deliveryGuard = Objects.requireNonNull(deliveryGuard, "deliveryGuard must not be null");
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat endpoint for testing AI responses")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<ChatResponse> chat(
      @RequestBody ChatRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    if (request.conversationId() != null) {
      ProcessConversationUseCase conversationUseCase =
          processConversation.orElseThrow(
              () -> new IllegalStateException("Durable conversation processing is unavailable"));
      var result =
          AiExecutionContextScope.call(
              conversationContext(request.conversationId(), idempotencyKey, jwt, authentication),
              () -> {
                var processed =
                    conversationUseCase.process(
                        new ProcessConversationCommand(
                            request.conversationId(), request.userMessage(), idempotencyKey));
                checkDelivery(processed.response());
                return processed;
              });
      return ResponseEntity.ok(
          new ChatResponse(result.response(), result.conversationId(), result.workflowId()));
    }
    String response =
        AiExecutionContextScope.call(
            readOnlyContext(jwt, authentication),
            () -> {
              String result =
                  chatUseCase.chat(
                      request.conversationContext() != null ? request.conversationContext() : "",
                      request.userMessage());
              checkDelivery(result);
              return result;
            });
    return ResponseEntity.ok(new ChatResponse(response));
  }

  @PostMapping("/intent")
  @Operation(summary = "Detect intent from user message")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<IntentResult> detectIntent(
      @RequestBody IntentRequest request,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    IntentResult result =
        AiExecutionContextScope.call(
            readOnlyContext(jwt, authentication), () -> detectIntent.detect(request.message()));
    return ResponseEntity.ok(result);
  }

  @PostMapping("/rag")
  @Operation(summary = "RAG query against tenant documents")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<RagResponse> ragQuery(
      @RequestBody RagRequest request,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    return AiExecutionContextScope.call(
        readOnlyContext(jwt, authentication),
        () -> {
          String response = ragQuery.query(request.question());
          checkDelivery(response);
          return ResponseEntity.ok(new RagResponse(response));
        });
  }

  private void checkDelivery(String response) {
    if (deliveryGuard.isEmpty()) {
      return;
    }
    var decision =
        deliveryGuard
            .orElseThrow()
            .check(
                new DeliveryRequest(
                    com.emme.kernel.context.Channel.WEB.name().toLowerCase(Locale.ROOT),
                    response,
                    UNBOUNDED_WEB_RESPONSE_CHARACTERS,
                    false),
                AiExecutionContextScope.requireCurrent());
    if (decision.action() != GuardrailAction.DELIVER) {
      throw new GuardrailRejectedException(decision);
    }
  }

  private AiExecutionContext readOnlyContext(Jwt jwt, Authentication authentication) {
    String traceId = requireCorrelationId();
    Jwt authenticatedJwt = Objects.requireNonNull(jwt, "Authenticated JWT is required");
    Authentication authenticatedPrincipal =
        Objects.requireNonNull(authentication, "Authenticated principal is required");
    return contextFactory.forReadOnly(
        traceId,
        Objects.requireNonNull(authenticatedJwt.getIssuer(), "JWT issuer is required").toString(),
        authenticatedJwt.getSubject(),
        authenticatedPrincipal.getAuthorities());
  }

  private AiExecutionContext conversationContext(
      java.util.UUID conversationId,
      String idempotencyKey,
      Jwt jwt,
      Authentication authentication) {
    Jwt authenticatedJwt = Objects.requireNonNull(jwt, "Authenticated JWT is required");
    Authentication authenticatedPrincipal =
        Objects.requireNonNull(authentication, "Authenticated principal is required");
    return contextFactory.forConversation(
        conversationId,
        requireCorrelationId(),
        idempotencyKey,
        Objects.requireNonNull(authenticatedJwt.getIssuer(), "JWT issuer is required").toString(),
        authenticatedJwt.getSubject(),
        authenticatedPrincipal.getAuthorities());
  }

  private static String requireCorrelationId() {
    String traceId = CorrelationId.get();
    if (traceId == null || traceId.isBlank()) {
      throw new IllegalStateException("Correlation ID is required for AI request");
    }
    return traceId;
  }
}
