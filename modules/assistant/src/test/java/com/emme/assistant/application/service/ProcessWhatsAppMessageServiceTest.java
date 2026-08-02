package com.emme.assistant.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.assistant.domain.model.ConversationStatus;
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
  private final ProcessWhatsAppMessageService service =
      new ProcessWhatsAppMessageService(
          startConversation,
          listConversations,
          addConversationEvent,
          chatUseCase,
          participantRepository,
          webhookEvents,
          replyPort);

  private UUID tenantId;
  private UUID participantId;
  private ConversationInfo conversation;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    participantId = UUID.randomUUID();
    conversation =
        new ConversationInfo(
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
}
