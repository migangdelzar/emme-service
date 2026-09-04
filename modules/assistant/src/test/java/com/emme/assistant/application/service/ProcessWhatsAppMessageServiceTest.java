package com.emme.assistant.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.assistant.api.event.WhatsAppMessageReceived;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.type.ConversationStatus;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.application.port.out.WhatsAppMessageEventPublisher;
import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.type.ChannelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessWhatsAppMessageServiceTest {

  private final StartConversationUseCase startConversation = org.mockito.Mockito.mock();
  private final ListConversationsUseCase listConversations = org.mockito.Mockito.mock();
  private final AddConversationEventUseCase addConversationEvent = org.mockito.Mockito.mock();
  private final ChatUseCase chatUseCase = org.mockito.Mockito.mock();
  private final ChannelParticipantRepository participantRepository = org.mockito.Mockito.mock();
  private final WhatsAppWebhookEventRepository webhookEvents = org.mockito.Mockito.mock();
  private final WhatsAppReplyPort replyPort = org.mockito.Mockito.mock();
  private final WhatsAppMessageEventPublisher eventPublisher = org.mockito.Mockito.mock();
  private final ProcessWhatsAppMessageService service =
      new ProcessWhatsAppMessageService(
          startConversation,
          listConversations,
          addConversationEvent,
          chatUseCase,
          participantRepository,
          webhookEvents,
          replyPort,
          eventPublisher);

  private UUID tenantId;
  private UUID participantId;
  private ConversationDetails conversation;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    participantId = UUID.randomUUID();
    conversation =
        new ConversationDetails(
            UUID.randomUUID(),
            tenantId,
            participantId,
            ChannelType.WHATSAPP,
            ConversationStatus.ACTIVE,
            java.time.Instant.now());
  }

  @Test
  void claimsEventPersistsConversationMessagesAndSendsReply() {
    ProcessWhatsAppMessageCommand command =
        new ProcessWhatsAppMessageCommand(tenantId, "event-1", "phone", "hello");
    ChannelParticipant participant =
        new ChannelParticipant(
            participantId,
            tenantId,
            ChannelType.WHATSAPP,
            "phone",
            null,
            com.emme.assistant.domain.model.ConsentStatus.UNKNOWN);
    when(webhookEvents.claim(tenantId, "whatsapp", "event-1")).thenReturn(true);
    when(participantRepository.findByTenantIdAndChannelAndProviderReference(
            tenantId, ChannelType.WHATSAPP, "phone"))
        .thenReturn(Optional.of(participant));
    when(listConversations.list(any())).thenReturn(List.of(conversation));
    when(chatUseCase.chat("", "hello")).thenReturn("reply");

    service.process(command);

    verify(addConversationEvent, org.mockito.Mockito.times(2)).add(any());
    verify(replyPort).send("phone", "reply");
  }

  @Test
  void ignoresAlreadyClaimedEventWithoutInvokingTheWorkflow() {
    ProcessWhatsAppMessageCommand command =
        new ProcessWhatsAppMessageCommand(tenantId, "event-1", "phone", "hello");
    when(webhookEvents.claim(tenantId, "whatsapp", "event-1")).thenReturn(false);

    service.process(command);

    verify(participantRepository, never())
        .findByTenantIdAndChannelAndProviderReference(any(), any(), any());
    verify(chatUseCase, never()).chat(any(), any());
    verify(replyPort, never()).send(any(), any());
  }

  @Test
  void enqueuesAValidatedMessageWithoutRunningTheModelInline() {
    ProcessWhatsAppMessageCommand command =
        new ProcessWhatsAppMessageCommand(tenantId, "event-2", "phone", "hello");

    com.emme.kernel.context.TenantContextHolder.withTenantOverride(
        tenantId, () -> service.enqueue(command));

    verify(eventPublisher).publish(new WhatsAppMessageReceived(command));
    verify(webhookEvents, never()).claim(any(), any(), any());
    verify(chatUseCase, never()).chat(any(), any());
    verify(replyPort, never()).send(any(), any());
  }

  @Test
  void rebindsTheTrustedTenantAndAiContextOnTheAsynchronousWorkerPath() {
    ProcessWhatsAppMessageCommand command =
        new ProcessWhatsAppMessageCommand(tenantId, "event-3", "phone", "hello");
    ChannelParticipant participant =
        new ChannelParticipant(
            participantId,
            tenantId,
            ChannelType.WHATSAPP,
            "phone",
            null,
            com.emme.assistant.domain.model.ConsentStatus.UNKNOWN);
    when(webhookEvents.claim(tenantId, "whatsapp", "event-3")).thenReturn(true);
    when(participantRepository.findByTenantIdAndChannelAndProviderReference(
            tenantId, ChannelType.WHATSAPP, "phone"))
        .thenReturn(Optional.of(participant));
    when(listConversations.list(any())).thenReturn(List.of(conversation));
    when(chatUseCase.chat("", "hello"))
        .thenAnswer(
            invocation -> {
              org.assertj.core.api.Assertions.assertThat(
                      AiExecutionContextScope.requireCurrent().tenantId())
                  .isEqualTo(tenantId);
              org.assertj.core.api.Assertions.assertThat(
                      com.emme.kernel.context.TenantContextHolder.requireCurrentTenantId())
                  .isEqualTo(tenantId);
              return "reply";
            });

    service.processReceivedMessage(new WhatsAppMessageReceived(command, null));

    verify(replyPort).send("phone", "reply");
    org.assertj.core.api.Assertions.assertThat(AiExecutionContextScope.current()).isEmpty();
  }
}
