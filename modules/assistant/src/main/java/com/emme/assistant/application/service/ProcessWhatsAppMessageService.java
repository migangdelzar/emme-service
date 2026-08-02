package com.emme.assistant.application.service;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.ProcessWhatsAppMessageUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.assistant.domain.model.ConsentStatus;
import com.emme.assistant.domain.model.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates the single application use case for inbound WhatsApp messages. */
@Service
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
@Transactional
public class ProcessWhatsAppMessageService implements ProcessWhatsAppMessageUseCase {

  private final StartConversationUseCase startConversation;
  private final ListConversationsUseCase listConversations;
  private final AddConversationEventUseCase addConversationEvent;
  private final ChatUseCase chatUseCase;
  private final ChannelParticipantRepository participantRepository;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final WhatsAppReplyPort replyPort;

  public ProcessWhatsAppMessageService(
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppReplyPort replyPort) {
    this.startConversation = startConversation;
    this.listConversations = listConversations;
    this.addConversationEvent = addConversationEvent;
    this.chatUseCase = chatUseCase;
    this.participantRepository = participantRepository;
    this.webhookEvents = webhookEvents;
    this.replyPort = replyPort;
  }

  @Override
  public void process(ProcessWhatsAppMessageCommand command) {
    if (command.eventId() == null || command.eventId().isBlank()) {
      throw new SecurityException("WhatsApp webhook event id is required");
    }
    if (!webhookEvents.claim(command.tenantId(), "whatsapp", command.eventId())) {
      return;
    }

    ChannelParticipant participant = findOrCreateParticipant(command);
    ConversationInfo conversation = findOrCreateConversation(command, participant);
    addConversationEvent.add(
        new AddConversationEventCommand(conversation.id(), "MESSAGE_RECEIVED", command.text()));

    String response = chatUseCase.chat("", command.text());
    addConversationEvent.add(
        new AddConversationEventCommand(conversation.id(), "MESSAGE_SENT", response));
    replyPort.send(command.from(), response);
  }

  private ChannelParticipant findOrCreateParticipant(ProcessWhatsAppMessageCommand command) {
    return participantRepository
        .findByTenantIdAndChannelAndProviderReference(
            command.tenantId(), ChannelType.WHATSAPP, command.from())
        .orElseGet(
            () ->
                participantRepository.save(
                    new ChannelParticipant(
                        null,
                        command.tenantId(),
                        ChannelType.WHATSAPP,
                        command.from(),
                        null,
                        ConsentStatus.UNKNOWN)));
  }

  private ConversationInfo findOrCreateConversation(
      ProcessWhatsAppMessageCommand command, ChannelParticipant participant) {
    List<ConversationInfo> conversations =
        listConversations.list(new ListConversationsQuery(command.tenantId()));
    return conversations.stream()
        .filter(conversation -> conversation.participantId().equals(participant.id()))
        .filter(conversation -> conversation.status() == ConversationStatus.ACTIVE)
        .findFirst()
        .orElseGet(
            () ->
                startConversation.start(
                    new StartConversationCommand(
                        command.tenantId(), participant.id(), ChannelType.WHATSAPP)));
  }
}
