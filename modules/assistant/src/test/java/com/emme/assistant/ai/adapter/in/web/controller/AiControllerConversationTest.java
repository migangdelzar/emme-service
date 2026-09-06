package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.in.web.request.ChatRequest;
import com.emme.assistant.ai.adapter.in.web.response.ChatResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.api.usecase.ProcessConversationUseCase;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import com.emme.assistant.ai.application.guardrail.DeliveryGuard;
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

class AiControllerConversationTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @AfterEach
  void clearCorrelationId() {
    CorrelationId.clear();
  }

  @Test
  void createsAConversationCommandFromTrustedContextWithoutAcceptingTenantId() {
    ChatUseCase legacyChat = mock(ChatUseCase.class);
    DetectIntentUseCase intent = mock(DetectIntentUseCase.class);
    RagQueryUseCase rag = mock(RagQueryUseCase.class);
    ProcessConversationUseCase conversations = mock(ProcessConversationUseCase.class);
    when(conversations.process(any(ProcessConversationCommand.class)))
        .thenReturn(new ProcessConversationResult(CONVERSATION_ID, WORKFLOW_ID, "answer"));
    AiController controller =
        new AiController(
            legacyChat, intent, rag, conversations, new AiWebExecutionContextFactory());
    Jwt jwt = jwt();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_client")));
    CorrelationId.set("trace-conversation-1");

    ResponseEntity<?> response =
        TenantContextHolder.withTenantOverride(
            TENANT_ID,
            () ->
                controller.chat(
                    new ChatRequest("question", null, CONVERSATION_ID),
                    "conversation-turn-1",
                    jwt,
                    authentication));

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody())
        .isEqualTo(new ChatResponse("answer", CONVERSATION_ID, WORKFLOW_ID));
    verify(conversations)
        .process(
            new ProcessConversationCommand(CONVERSATION_ID, "question", "conversation-turn-1"));
  }

  @Test
  void keepsLegacyControllerConstructionAndChatResponseCompatible() {
    ChatUseCase legacyChat = mock(ChatUseCase.class);
    DetectIntentUseCase intent = mock(DetectIntentUseCase.class);
    RagQueryUseCase rag = mock(RagQueryUseCase.class);
    when(legacyChat.chat("", "question")).thenReturn("answer");
    AiController controller =
        new AiController(legacyChat, intent, rag, new AiWebExecutionContextFactory());
    Jwt jwt = jwt();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_client")));
    CorrelationId.set("trace-legacy-1");

    ResponseEntity<ChatResponse> response =
        TenantContextHolder.withTenantOverride(
            TENANT_ID,
            () -> controller.chat(new ChatRequest("question", null), null, jwt, authentication));

    assertThat(response.getBody()).isEqualTo(new ChatResponse("answer"));
    verify(legacyChat).chat("", "question");
  }

  @Test
  void doesNotReturnAConversationResponseWhenWebDeliveryIsRejected() {
    ChatUseCase legacyChat = mock(ChatUseCase.class);
    DetectIntentUseCase intent = mock(DetectIntentUseCase.class);
    RagQueryUseCase rag = mock(RagQueryUseCase.class);
    ProcessConversationUseCase conversations = mock(ProcessConversationUseCase.class);
    DeliveryGuard delivery = mock(DeliveryGuard.class);
    when(conversations.process(any(ProcessConversationCommand.class)))
        .thenReturn(new ProcessConversationResult(CONVERSATION_ID, WORKFLOW_ID, "answer"));
    when(delivery.check(any(), any()))
        .thenReturn(
            new com.emme.ai.contracts.guardrail.GuardrailDecision(
                com.emme.ai.contracts.guardrail.GuardrailAction.BLOCK,
                "delivery.rejected",
                java.util.Map.of()));
    AiController controller =
        new AiController(
            legacyChat,
            intent,
            rag,
            conversations,
            new AiWebExecutionContextFactory(),
            java.util.Optional.of(delivery));
    Jwt jwt = jwt();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            jwt, null, List.of(new SimpleGrantedAuthority("ROLE_tenant_client")));
    CorrelationId.set("trace-conversation-rejected");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                TenantContextHolder.withTenantOverride(
                    TENANT_ID,
                    () ->
                        controller.chat(
                            new ChatRequest("question", null, CONVERSATION_ID),
                            "conversation-turn-rejected",
                            jwt,
                            authentication)))
        .isInstanceOf(com.emme.assistant.ai.application.guardrail.GuardrailRejectedException.class);
  }

  private static Jwt jwt() {
    return Jwt.withTokenValue("token")
        .issuer("https://issuer.example/realms/emme")
        .subject("auth0|client-123")
        .header("alg", "none")
        .build();
  }
}
