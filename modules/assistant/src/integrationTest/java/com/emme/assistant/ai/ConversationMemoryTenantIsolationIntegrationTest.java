package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.type.ChannelType;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("conversation memory tenant isolation integration")
class ConversationMemoryTenantIsolationIntegrationTest {

  @Autowired private ConversationMemoryPort memory;
  @Autowired private StartConversationUseCase startConversation;
  @Autowired private GetConversationHistoryUseCase conversationHistory;

  @Test
  @DisplayName("tenant A cannot load, read, or append to tenant B conversation")
  void deniesAllConversationMemoryOperationsOutsideTheAuthenticatedTenant() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    ConversationDetails conversation =
        startConversation.start(
            new StartConversationCommand(tenantB, UUID.randomUUID(), ChannelType.WEB_CHAT));
    AiExecutionContext tenantAContext =
        new AiExecutionContext(
            tenantA,
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            conversation.id(),
            UUID.randomUUID(),
            "trace-tenant-isolation",
            "turn-tenant-isolation");

    AiExecutionContextScope.run(
        tenantAContext,
        () -> {
          assertThatThrownBy(() -> memory.load(conversation.id(), tenantAContext))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Conversation is not accessible for the authenticated tenant");
          assertThatThrownBy(
                  () -> memory.appendUserMessage(conversation.id(), "question", tenantAContext))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Conversation is not accessible for the authenticated tenant");
          assertThatThrownBy(
                  () ->
                      memory.appendAssistantMessage(
                          conversation.id(), "answer", "turn-tenant-isolation", tenantAContext))
              .isInstanceOf(SecurityException.class)
              .hasMessage("Conversation is not accessible for the authenticated tenant");
        });

    assertThat(conversationHistory.get(new GetConversationHistoryQuery(tenantB, conversation.id())))
        .isEmpty();
  }

  @Test
  @DisplayName("stores and retrieves an assistant finalization marker within the tenant")
  void persistsAssistantResponseByConversationTurnIdempotencyKey() {
    UUID tenantId = UUID.randomUUID();
    ConversationDetails conversation =
        startConversation.start(
            new StartConversationCommand(tenantId, UUID.randomUUID(), ChannelType.WEB_CHAT));
    AiExecutionContext context =
        new AiExecutionContext(
            tenantId,
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT"),
            conversation.id(),
            UUID.randomUUID(),
            "trace-finalization-marker",
            "turn-finalization-marker");

    AiExecutionContextScope.run(
        context,
        () ->
            memory.appendAssistantMessage(
                conversation.id(), "answer", "turn-finalization-marker", context));

    assertThat(
            AiExecutionContextScope.call(
                context,
                () ->
                    memory.findAssistantResponse(
                        conversation.id(), "turn-finalization-marker", context)))
        .contains("answer");
  }
}
