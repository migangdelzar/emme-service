package com.emme.assistant.application.service;

import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.tenant.AiAuthorizationContextResolver;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.guardrail.DeliveryGuard;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.event.WhatsAppMessageReceived;
import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.type.ConversationStatus;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.ProcessWhatsAppMessageUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.application.port.out.WhatsAppMessageEventPublisher;
import com.emme.assistant.application.port.out.WhatsAppReplyPort;
import com.emme.assistant.application.port.out.WhatsAppWebhookEventRepository;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.assistant.domain.model.ConsentStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.Channel;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.ChannelType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates the single application use case for inbound WhatsApp messages. */
@Service
@ConditionalOnExpression("not '${app.whatsapp.verify-token:}'.isEmpty()")
@Transactional
public class ProcessWhatsAppMessageService implements ProcessWhatsAppMessageUseCase {

  private static final int MAXIMUM_TEXT_CHARACTERS = 4096;

  private final StartConversationUseCase startConversation;
  private final ListConversationsUseCase listConversations;
  private final AddConversationEventUseCase addConversationEvent;
  private final ChatUseCase chatUseCase;
  private final ChannelParticipantRepository participantRepository;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final WhatsAppReplyPort replyPort;
  private final WhatsAppMessageEventPublisher eventPublisher;
  private final java.util.Optional<AiAuthorizationContextResolver> authorizationResolver;
  private final java.util.Optional<DeliveryGuard> deliveryGuard;

  public ProcessWhatsAppMessageService(
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppReplyPort replyPort) {
    this(
        startConversation,
        listConversations,
        addConversationEvent,
        chatUseCase,
        participantRepository,
        webhookEvents,
        replyPort,
        event -> {},
        java.util.Optional.empty(),
        java.util.Optional.empty());
  }

  public ProcessWhatsAppMessageService(
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppReplyPort replyPort,
      WhatsAppMessageEventPublisher eventPublisher) {
    this(
        startConversation,
        listConversations,
        addConversationEvent,
        chatUseCase,
        participantRepository,
        webhookEvents,
        replyPort,
        eventPublisher,
        java.util.Optional.empty(),
        java.util.Optional.empty());
  }

  public ProcessWhatsAppMessageService(
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppReplyPort replyPort,
      WhatsAppMessageEventPublisher eventPublisher,
      java.util.Optional<AiAuthorizationContextResolver> authorizationResolver) {
    this(
        startConversation,
        listConversations,
        addConversationEvent,
        chatUseCase,
        participantRepository,
        webhookEvents,
        replyPort,
        eventPublisher,
        authorizationResolver,
        java.util.Optional.empty());
  }

  @Autowired
  public ProcessWhatsAppMessageService(
      StartConversationUseCase startConversation,
      ListConversationsUseCase listConversations,
      AddConversationEventUseCase addConversationEvent,
      ChatUseCase chatUseCase,
      ChannelParticipantRepository participantRepository,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppReplyPort replyPort,
      WhatsAppMessageEventPublisher eventPublisher,
      java.util.Optional<AiAuthorizationContextResolver> authorizationResolver,
      java.util.Optional<DeliveryGuard> deliveryGuard) {
    this.startConversation = startConversation;
    this.listConversations = listConversations;
    this.addConversationEvent = addConversationEvent;
    this.chatUseCase = chatUseCase;
    this.participantRepository = participantRepository;
    this.webhookEvents = webhookEvents;
    this.replyPort = replyPort;
    this.eventPublisher = eventPublisher;
    this.authorizationResolver =
        java.util.Objects.requireNonNull(
            authorizationResolver, "authorizationResolver must not be null");
    this.deliveryGuard =
        java.util.Objects.requireNonNull(deliveryGuard, "deliveryGuard must not be null");
  }

  @Override
  public void enqueue(ProcessWhatsAppMessageCommand command) {
    validate(command);
    UUID currentTenant = TenantContextHolder.requireCurrentTenantId();
    if (!currentTenant.equals(command.tenantId())) {
      throw new SecurityException("WhatsApp tenant does not match the backend context");
    }
    eventPublisher.publish(new WhatsAppMessageReceived(command));
  }

  @Override
  public void process(ProcessWhatsAppMessageCommand command) {
    validate(command);
    UUID currentTenant = TenantContextHolder.currentTenantOptional().orElse(null);
    AiExecutionContext currentContext = AiExecutionContextScope.current().orElse(null);
    if (currentTenant != null && !currentTenant.equals(command.tenantId())) {
      throw new SecurityException("WhatsApp tenant does not match the backend context");
    }
    if (currentContext != null && !currentContext.tenantId().equals(command.tenantId())) {
      throw new SecurityException("WhatsApp tenant does not match the AI context");
    }
    if (currentTenant == null || currentContext == null) {
      bindSystemContext(
          command,
          TenantContextHolder.currentDatabaseOptional().orElse(null),
          () -> processInCurrentContext(command));
      return;
    }
    processInCurrentContext(command);
  }

  private void processInCurrentContext(ProcessWhatsAppMessageCommand command) {
    if (!webhookEvents.claim(command.tenantId(), "whatsapp", command.eventId())) {
      return;
    }

    ChannelParticipant participant = findOrCreateParticipant(command);
    ConversationDetails conversation = findOrCreateConversation(command, participant);
    addConversationEvent.add(
        new AddConversationEventCommand(
            command.tenantId(), conversation.id(), "MESSAGE_RECEIVED", command.text()));

    String response = chatInResolvedAuthorization(command, participant);
    checkDelivery(response);
    addConversationEvent.add(
        new AddConversationEventCommand(
            command.tenantId(), conversation.id(), "MESSAGE_SENT", response));
    replyPort.send(command.from(), response);
  }

  private void checkDelivery(String response) {
    if (deliveryGuard.isEmpty()) {
      return;
    }
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    GuardrailDecision decision =
        deliveryGuard
            .orElseThrow()
            .check(
                new DeliveryRequest(
                    Channel.WHATSAPP.name().toLowerCase(Locale.ROOT),
                    response,
                    MAXIMUM_TEXT_CHARACTERS,
                    false),
                context);
    if (decision.action() != GuardrailAction.DELIVER) {
      throw new GuardrailRejectedException(decision);
    }
  }

  private String chatInResolvedAuthorization(
      ProcessWhatsAppMessageCommand command, ChannelParticipant participant) {
    if (authorizationResolver.isEmpty()) {
      return chatUseCase.chat("", command.text());
    }
    Set<String> authenticatedRoles = participant.customerId() == null ? Set.of() : Set.of("client");
    String principalReference =
        participant.customerId() == null ? command.from() : participant.customerId().toString();
    var authorization =
        authorizationResolver
            .orElseThrow()
            .resolve(command.tenantId(), principalReference, authenticatedRoles, Channel.WHATSAPP);
    AiExecutionContext current = AiExecutionContextScope.requireCurrent();
    AiExecutionContext authorized =
        new AiExecutionContext(
            current.tenantId(),
            current.principalId(),
            authorization.roles(),
            current.conversationId(),
            current.workflowId(),
            current.traceId(),
            current.idempotencyKey(),
            Channel.WHATSAPP,
            authorization.tenantCapabilities(),
            authorization.enabledFeatures());
    return AiExecutionContextScope.call(authorized, () -> chatUseCase.chat("", command.text()));
  }

  private void bindSystemContext(
      ProcessWhatsAppMessageCommand command, UUID databaseId, Runnable action) {
    String traceId = "whatsapp:" + command.eventId();
    UUID resourceId =
        UUID.nameUUIDFromBytes(
            ("emme-whatsapp:" + command.eventId()).getBytes(StandardCharsets.UTF_8));
    AiExecutionContext context =
        new AiExecutionContext(
            command.tenantId(),
            UUID.nameUUIDFromBytes(
                ("emme-whatsapp-system:" + command.tenantId()).getBytes(StandardCharsets.UTF_8)),
            Set.of("ROLE_system_whatsapp"),
            resourceId,
            resourceId,
            traceId,
            command.eventId(),
            Channel.WHATSAPP,
            Set.of(),
            Set.of());
    TenantContextHolder.withTenantAndCorrelation(
        command.tenantId(),
        databaseId,
        traceId,
        () -> AiExecutionContextScope.run(context, action::run));
  }

  private static void validate(ProcessWhatsAppMessageCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("WhatsApp message command is required");
    }
    if (command.tenantId() == null) {
      throw new SecurityException("WhatsApp tenant is required");
    }
    if (command.eventId() == null || command.eventId().isBlank()) {
      throw new SecurityException("WhatsApp webhook event id is required");
    }
    if (command.from() == null || command.from().isBlank()) {
      throw new SecurityException("WhatsApp sender is required");
    }
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

  private ConversationDetails findOrCreateConversation(
      ProcessWhatsAppMessageCommand command, ChannelParticipant participant) {
    List<ConversationDetails> conversations =
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
