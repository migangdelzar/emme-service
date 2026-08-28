package com.emme.assistant.ai.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.assistant.ai.adapter.in.web.request.ChatRequest;
import com.emme.assistant.ai.adapter.in.web.request.IntentRequest;
import com.emme.assistant.ai.adapter.in.web.request.RagRequest;
import com.emme.assistant.ai.adapter.in.web.response.ChatResponse;
import com.emme.assistant.ai.adapter.in.web.response.RagResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.tracing.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai", version = "1.0")
@Tag(name = "AI")
public class AiController {

  private final ChatUseCase chatUseCase;
  private final DetectIntentUseCase detectIntent;
  private final RagQueryUseCase ragQuery;
  private final AiWebExecutionContextFactory contextFactory;

  public AiController(
      ChatUseCase chatUseCase,
      DetectIntentUseCase detectIntent,
      RagQueryUseCase ragQuery,
      AiWebExecutionContextFactory contextFactory) {
    this.chatUseCase = chatUseCase;
    this.detectIntent = detectIntent;
    this.ragQuery = ragQuery;
    this.contextFactory = contextFactory;
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat endpoint for testing AI responses")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    String response =
        chatUseCase.chat(
            request.conversationContext() != null ? request.conversationContext() : "",
            request.userMessage());
    return ResponseEntity.ok(new ChatResponse(response));
  }

  @PostMapping("/intent")
  @Operation(summary = "Detect intent from user message")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<IntentResult> detectIntent(
      @RequestBody IntentRequest request,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication) {
    String traceId = CorrelationId.get();
    if (traceId == null || traceId.isBlank()) {
      throw new IllegalStateException("Correlation ID is required for AI intent detection");
    }
    Jwt authenticatedJwt = Objects.requireNonNull(jwt, "Authenticated JWT is required");
    Authentication authenticatedPrincipal =
        Objects.requireNonNull(authentication, "Authenticated principal is required");
    var context =
        contextFactory.forReadOnly(
            traceId,
            Objects.requireNonNull(authenticatedJwt.getIssuer(), "JWT issuer is required")
                .toString(),
            authenticatedJwt.getSubject(),
            authenticatedPrincipal.getAuthorities());
    IntentResult result =
        AiExecutionContextScope.call(context, () -> detectIntent.detect(request.message()));
    return ResponseEntity.ok(result);
  }

  @PostMapping("/rag")
  @Operation(summary = "RAG query against tenant documents")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<RagResponse> ragQuery(@RequestBody RagRequest request) {
    return withCurrentTenant(
        tenantId -> {
          String answer = ragQuery.query(tenantId, request.question());
          return ResponseEntity.ok(new RagResponse(answer));
        });
  }
}
